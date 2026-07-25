package com.wrenchlog.wrenchlog.controller;

import com.wrenchlog.wrenchlog.dto.ServiceLogResponseDTO;
import com.wrenchlog.wrenchlog.model.ServiceLog;
import com.wrenchlog.wrenchlog.model.User;
import com.wrenchlog.wrenchlog.model.Vehicle;
import com.wrenchlog.wrenchlog.repository.ServiceLogRepository;
import com.wrenchlog.wrenchlog.repository.VehicleRepository;
import com.wrenchlog.wrenchlog.service.VehicleAccessService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/services")
public class ServiceLogController {
    private final VehicleAccessService vehicleAccessService;
    private final ServiceLogRepository serviceLogRepository;

    public ServiceLogController(ServiceLogRepository serviceLogRepository,
                                VehicleAccessService vehicleAccessService) {
        this.serviceLogRepository = serviceLogRepository;
        this.vehicleAccessService = vehicleAccessService;
    }

    @GetMapping
    public List<ServiceLogResponseDTO> getServicesForVehicle(@RequestParam Long vehicleId,
                                                             @AuthenticationPrincipal User user) {
        vehicleAccessService.getOwnedVehicleOrThrow(vehicleId, user);

        return serviceLogRepository.findByVehicleId(vehicleId)
                .stream()
                .map(log -> new ServiceLogResponseDTO(
                        log.getId(),
                        log.getDescription(),
                        log.getCost(),
                        log.getKilometersAtService(),
                        log.getServiceDate(),
                        log.getVehicle().getId()))
                .toList();
    }

    @PostMapping
    public ResponseEntity<ServiceLogResponseDTO> addServiceLog(
            @RequestParam Long vehicleId,
            @RequestBody ServiceLog serviceLog,
            @AuthenticationPrincipal User user
    ) {
        Vehicle vehicle = vehicleAccessService.getOwnedVehicleOrThrow(vehicleId, user);

        serviceLog.setVehicle(vehicle);
        ServiceLog savedLog = serviceLogRepository.save(serviceLog);

        ServiceLogResponseDTO response = new ServiceLogResponseDTO(
                savedLog.getId(),
                savedLog.getDescription(),
                savedLog.getCost(),
                savedLog.getKilometersAtService(),
                savedLog.getServiceDate(),
                vehicle.getId());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServiceLogResponseDTO> modifyServiceLog(
            @PathVariable Long id,
            @RequestParam Long vehicleId,
            @RequestBody ServiceLog serviceLog,
            @AuthenticationPrincipal User user
    ) {
        ServiceLog existing = serviceLogRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        vehicleAccessService.assertOwnership(existing.getVehicle(), user);

        Vehicle vehicle = vehicleAccessService.getOwnedVehicleOrThrow(vehicleId, user);

        serviceLog.setVehicle(vehicle);
        serviceLog.setId(id);
        ServiceLog savedLog = serviceLogRepository.save(serviceLog);

        ServiceLogResponseDTO response = new ServiceLogResponseDTO(
                savedLog.getId(),
                savedLog.getDescription(),
                savedLog.getCost(),
                savedLog.getKilometersAtService(),
                savedLog.getServiceDate(),
                vehicle.getId());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteServiceLog(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        ServiceLog log = serviceLogRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        vehicleAccessService.assertOwnership(log.getVehicle(), user);

        serviceLogRepository.deleteById(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}