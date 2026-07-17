package com.wrenchlog.wrenchlog.controller;

import com.wrenchlog.wrenchlog.dto.ServiceLogResponseDTO;
import com.wrenchlog.wrenchlog.model.ServiceLog;
import com.wrenchlog.wrenchlog.repository.ServiceLogRepository;
import com.wrenchlog.wrenchlog.repository.VehicleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/services")
public class ServiceLogController {
    private final VehicleRepository vehicleRepository;
    private final ServiceLogRepository serviceLogRepository;

    public ServiceLogController(VehicleRepository vehicleRepository, ServiceLogRepository serviceLogRepository) {
        this.vehicleRepository = vehicleRepository;
        this.serviceLogRepository = serviceLogRepository;
    }

    @GetMapping
    public List<ServiceLogResponseDTO> getServicesForVehicle(@RequestParam Long vehicleId) {
        List<ServiceLog> logs = serviceLogRepository.findByVehicleId(vehicleId);
        return logs.stream()
                .map(log -> new ServiceLogResponseDTO(
                        log.getId(),
                        log.getDescription(),
                        log.getCost(),
                        log.getKilometersAtService(),
                        log.getServiceDate(),
                        log.getVehicle().getId()
                ))
                .toList();
    }

    @PostMapping
    public ResponseEntity<ServiceLogResponseDTO> addServiceLog(@RequestParam Long vehicleId, @RequestBody ServiceLog serviceLog) {
        return vehicleRepository.findById(vehicleId)
                .map(vehicle -> {
                    serviceLog.setVehicle(vehicle);
                    ServiceLog savedLog = serviceLogRepository.save(serviceLog);

                    ServiceLogResponseDTO response = new ServiceLogResponseDTO(
                            savedLog.getId(),
                            savedLog.getDescription(),
                            savedLog.getCost(),
                            savedLog.getKilometersAtService(),
                            savedLog.getServiceDate(),
                            vehicle.getId()
                    );
                    return new ResponseEntity<>(response, HttpStatus.CREATED);
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehicle not found"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServiceLogResponseDTO> modifyServiceLog(@PathVariable Long id, @RequestParam("id") Long vehicleId, @RequestBody ServiceLog serviceLog) {
        if (!serviceLogRepository.existsById(id)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return vehicleRepository.findById(vehicleId)
                .map(vehicle -> {
                    serviceLog.setVehicle(vehicle);
                    serviceLog.setId(id);
                    ServiceLog savedLog = serviceLogRepository.save(serviceLog);

                    ServiceLogResponseDTO response = new ServiceLogResponseDTO(
                            savedLog.getId(),
                            savedLog.getDescription(),
                            savedLog.getCost(),
                            savedLog.getKilometersAtService(),
                            savedLog.getServiceDate(),
                            vehicle.getId()
                    );
                    return new ResponseEntity<>(response, HttpStatus.OK);
                })
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteServiceLog(@PathVariable Long id) {
        if (serviceLogRepository.existsById(id)) {
            serviceLogRepository.deleteById(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}