package com.wrenchlog.wrenchlog.controller;

import com.wrenchlog.wrenchlog.dto.ServiceReminderCreateDTO;
import com.wrenchlog.wrenchlog.dto.ServiceReminderResponseDTO;
import com.wrenchlog.wrenchlog.model.ServiceReminder;
import com.wrenchlog.wrenchlog.model.User;
import com.wrenchlog.wrenchlog.model.Vehicle;
import com.wrenchlog.wrenchlog.repository.ServiceReminderRepository;
import com.wrenchlog.wrenchlog.service.VehicleAccessService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/reminders")
public class ServiceReminderController {
    private final VehicleAccessService vehicleAccessService;
    private final ServiceReminderRepository serviceReminderRepository;

    public ServiceReminderController(VehicleAccessService vehicleAccessService,
                                     ServiceReminderRepository serviceReminderRepository) {
        this.vehicleAccessService = vehicleAccessService;
        this.serviceReminderRepository = serviceReminderRepository;
    }

    private ServiceReminderResponseDTO toResponseDTO(ServiceReminder reminder) {
        return new ServiceReminderResponseDTO(
                reminder.getId(),
                reminder.getTitle(),
                reminder.getDescription(),
                reminder.getIntervalMonths(),
                reminder.getIntervalOdometer(),
                reminder.getLastServiceAtDate(),
                reminder.getLastServiceAtOdometer(),
                reminder.getCreatedAt(),
                reminder.getVehicle().getId()
        );
    }

    @GetMapping
    public List<ServiceReminderResponseDTO> getServiceRemindersForVehicle(@RequestParam Long vehicleId,
                                                                          @AuthenticationPrincipal User user){
        vehicleAccessService.getOwnedVehicleOrThrow(vehicleId, user);

        return serviceReminderRepository.findByVehicleId(vehicleId)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @PostMapping
    public ResponseEntity<ServiceReminderResponseDTO> addServiceReminder(
            @RequestParam Long vehicleId,
            @Valid @RequestBody ServiceReminderCreateDTO dto,
            @AuthenticationPrincipal User user
    ){
        Vehicle vehicle = vehicleAccessService.getOwnedVehicleOrThrow(vehicleId, user);

        ServiceReminder reminder = new ServiceReminder(
                dto.title(),
                dto.description(),
                dto.lastServiceAtOdometer(),
                dto.intervalOdometer(),
                dto.intervalMonths(),
                dto.lastServiceAtDate(),
                vehicle
        );

        ServiceReminder savedReminder = serviceReminderRepository.save(reminder);
        return new ResponseEntity<>(toResponseDTO(savedReminder), HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteServiceReminder(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ){
        ServiceReminder reminder = serviceReminderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        vehicleAccessService.assertOwnership(reminder.getVehicle(), user);

        serviceReminderRepository.deleteById(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServiceReminderResponseDTO> modifyServiceReminder(
            @PathVariable Long id,
            @RequestParam Long vehicleId,
            @Valid @RequestBody ServiceReminderCreateDTO dto,
            @AuthenticationPrincipal User user
    ){
        ServiceReminder existing = serviceReminderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        vehicleAccessService.assertOwnership(existing.getVehicle(), user);

        Vehicle vehicle = vehicleAccessService.getOwnedVehicleOrThrow(vehicleId, user);

        existing.setTitle(dto.title());
        existing.setDescription(dto.description());
        existing.setLastServiceAtOdometer(dto.lastServiceAtOdometer());
        existing.setIntervalOdometer(dto.intervalOdometer());
        existing.setIntervalMonths(dto.intervalMonths());
        existing.setLastServiceAtDate(dto.lastServiceAtDate());
        existing.setVehicle(vehicle);

        ServiceReminder savedReminder = serviceReminderRepository.save(existing);
        return new ResponseEntity<>(toResponseDTO(savedReminder), HttpStatus.OK);
    }
}