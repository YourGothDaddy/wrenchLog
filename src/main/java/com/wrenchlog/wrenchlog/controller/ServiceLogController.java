package com.wrenchlog.wrenchlog.controller;

import com.wrenchlog.wrenchlog.dto.ServiceLogCreateDTO;
import com.wrenchlog.wrenchlog.dto.ServiceLogResponseDTO;
import com.wrenchlog.wrenchlog.model.ServiceLog;
import com.wrenchlog.wrenchlog.model.User;
import com.wrenchlog.wrenchlog.model.Vehicle;
import com.wrenchlog.wrenchlog.repository.ServiceLogRepository;
import com.wrenchlog.wrenchlog.repository.VehicleRepository;
import com.wrenchlog.wrenchlog.service.VehicleAccessService;
import jakarta.validation.Valid;
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
    private final VehicleRepository vehicleRepository;

    public ServiceLogController(ServiceLogRepository serviceLogRepository,
                                VehicleAccessService vehicleAccessService,
                                VehicleRepository vehicleRepository) {
        this.serviceLogRepository = serviceLogRepository;
        this.vehicleAccessService = vehicleAccessService;
        this.vehicleRepository = vehicleRepository;
    }

    private ServiceLogResponseDTO toResponseDTO(ServiceLog log) {
        return new ServiceLogResponseDTO(
                log.getId(), log.getDescription(), log.getCost(),
                log.getKilometersAtService(), log.getServiceDate(), log.getVehicle().getId()
        );
    }

    private void advanceOdometerIfHigher(Vehicle vehicle, int kilometersAtService) {
        if (kilometersAtService > vehicle.getKilometers()) {
            vehicle.setKilometers(kilometersAtService);
            vehicleRepository.save(vehicle);
        }
    }

    @GetMapping
    public List<ServiceLogResponseDTO> getServicesForVehicle(@RequestParam Long vehicleId,
                                                             @AuthenticationPrincipal User user) {
        vehicleAccessService.getOwnedVehicleOrThrow(vehicleId, user);
        return serviceLogRepository.findByVehicleId(vehicleId).stream().map(this::toResponseDTO).toList();
    }

    @PostMapping
    public ResponseEntity<ServiceLogResponseDTO> addServiceLog(
            @RequestParam Long vehicleId,
            @Valid @RequestBody ServiceLogCreateDTO dto,
            @AuthenticationPrincipal User user
    ) {
        Vehicle vehicle = vehicleAccessService.getOwnedVehicleOrThrow(vehicleId, user);

        ServiceLog log = new ServiceLog(dto.description(), dto.cost(), dto.kilometersAtService(), dto.serviceDate(), vehicle);
        ServiceLog savedLog = serviceLogRepository.save(log);
        advanceOdometerIfHigher(vehicle, dto.kilometersAtService());

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponseDTO(savedLog));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServiceLogResponseDTO> modifyServiceLog(
            @PathVariable Long id,
            @RequestParam Long vehicleId,
            @Valid @RequestBody ServiceLogCreateDTO dto,
            @AuthenticationPrincipal User user
    ) {
        ServiceLog existing = serviceLogRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        vehicleAccessService.assertOwnership(existing.getVehicle(), user);

        Vehicle vehicle = vehicleAccessService.getOwnedVehicleOrThrow(vehicleId, user);

        existing.setDescription(dto.description());
        existing.setCost(dto.cost());
        existing.setKilometersAtService(dto.kilometersAtService());
        existing.setServiceDate(dto.serviceDate());
        existing.setVehicle(vehicle);

        ServiceLog savedLog = serviceLogRepository.save(existing);
        advanceOdometerIfHigher(vehicle, dto.kilometersAtService());

        return ResponseEntity.ok(toResponseDTO(savedLog));
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
        return ResponseEntity.noContent().build();
    }
}