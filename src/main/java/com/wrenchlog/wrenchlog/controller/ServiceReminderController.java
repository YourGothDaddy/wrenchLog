package com.wrenchlog.wrenchlog.controller;

import com.wrenchlog.wrenchlog.dto.ServiceReminderCreateDTO;
import com.wrenchlog.wrenchlog.dto.ServiceReminderResponseDTO;
import com.wrenchlog.wrenchlog.enums.ReminderSourceType;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

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
                reminder.getId(), reminder.getTitle(), reminder.getDescription(),
                reminder.getIntervalMonths(), reminder.getIntervalOdometer(),
                reminder.getLastServiceAtDate(), reminder.getLastServiceAtOdometer(),
                reminder.getCreatedAt(), reminder.getVehicle().getId(), reminder.getSourceType(),
                reminder.getVerifiedExpiryDate()
        );
    }

    @GetMapping
    public List<ServiceReminderResponseDTO> getServiceRemindersForVehicle(@RequestParam Long vehicleId,
                                                                          @AuthenticationPrincipal User user){
        vehicleAccessService.getOwnedVehicleOrThrow(vehicleId, user);
        return serviceReminderRepository.findByVehicleId(vehicleId).stream().map(this::toResponseDTO).toList();
    }

    @PostMapping
    public ResponseEntity<ServiceReminderResponseDTO> addServiceReminder(
            @RequestParam Long vehicleId,
            @Valid @RequestBody ServiceReminderCreateDTO dto,
            @AuthenticationPrincipal User user
    ){
        Vehicle vehicle = vehicleAccessService.getOwnedVehicleOrThrow(vehicleId, user);

        ReminderSourceType sourceType = dto.sourceType() != null ? dto.sourceType() : ReminderSourceType.MANUAL;

        ServiceReminder reminder = new ServiceReminder(
                dto.title(), dto.description(), dto.lastServiceAtOdometer(),
                dto.intervalOdometer(), dto.intervalMonths(), dto.lastServiceAtDate(), sourceType, vehicle
        );

        reminder.setVerifiedExpiryDate(dto.verifiedExpiryDate());

        ServiceReminder savedReminder = serviceReminderRepository.save(reminder);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponseDTO(savedReminder));
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
        return ResponseEntity.noContent().build();
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

        LocalDate previousLastServiceAtDate = existing.getLastServiceAtDate();

        existing.setTitle(dto.title());
        existing.setDescription(dto.description());
        existing.setLastServiceAtOdometer(dto.lastServiceAtOdometer());
        existing.setIntervalOdometer(dto.intervalOdometer());
        existing.setIntervalMonths(dto.intervalMonths());
        existing.setLastServiceAtDate(dto.lastServiceAtDate());
        existing.setVehicle(vehicle);

        boolean dateChanged = !Objects.equals(previousLastServiceAtDate, dto.lastServiceAtDate());
        if (dto.verifiedExpiryDate() != null) {
            existing.setVerifiedExpiryDate(dto.verifiedExpiryDate());
        } else if (dateChanged) {
            existing.setVerifiedExpiryDate(null);
        }

        ServiceReminder savedReminder = serviceReminderRepository.save(existing);
        return ResponseEntity.ok(toResponseDTO(savedReminder));
    }
}