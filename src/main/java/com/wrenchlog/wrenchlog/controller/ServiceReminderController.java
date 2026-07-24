package com.wrenchlog.wrenchlog.controller;

import com.wrenchlog.wrenchlog.model.ServiceReminder;
import com.wrenchlog.wrenchlog.model.User;
import com.wrenchlog.wrenchlog.model.Vehicle;
import com.wrenchlog.wrenchlog.repository.ServiceReminderRepository;
import com.wrenchlog.wrenchlog.repository.VehicleRepository;
import com.wrenchlog.wrenchlog.service.VehicleAccessService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
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

    @GetMapping
    public List<ServiceReminder> getServiceRemindersForVehicle(@RequestParam Long vehicleId,
                                                               @AuthenticationPrincipal User user){
        vehicleAccessService.getOwnedVehicleOrThrow(vehicleId, user);

        return serviceReminderRepository.findByVehicleId(vehicleId);
    }

    @PostMapping
    public ResponseEntity<ServiceReminder> addServiceReminder(
            @RequestParam Long vehicleId,
            @RequestBody ServiceReminder serviceReminder,
            @AuthenticationPrincipal User user
    ){
        Vehicle vehicle = vehicleAccessService.getOwnedVehicleOrThrow(vehicleId, user);

        serviceReminder.setVehicle(vehicle);
        serviceReminder.setCreatedAt(LocalDateTime.now());
        ServiceReminder savedReminder = serviceReminderRepository.save(serviceReminder);
        return new ResponseEntity<>(savedReminder, HttpStatus.CREATED);
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
    public ResponseEntity<ServiceReminder> modifyServiceReminder(
            @PathVariable Long id,
            @RequestParam Long vehicleId,
            @RequestBody ServiceReminder serviceReminder,
            @AuthenticationPrincipal User user
    ){
        ServiceReminder existing = serviceReminderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        vehicleAccessService.assertOwnership(existing.getVehicle(), user);

        Vehicle vehicle = vehicleAccessService.getOwnedVehicleOrThrow(vehicleId, user);

        serviceReminder.setVehicle(vehicle);
        serviceReminder.setId(id);
        serviceReminder.setCreatedAt(existing.getCreatedAt());
        ServiceReminder savedReminder = serviceReminderRepository.save(serviceReminder);
        return new ResponseEntity<>(savedReminder, HttpStatus.OK);
    }
}
