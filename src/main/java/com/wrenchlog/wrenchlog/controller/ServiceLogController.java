package com.wrenchlog.wrenchlog.controller;

import com.wrenchlog.wrenchlog.model.ServiceLog;
import com.wrenchlog.wrenchlog.repository.ServiceLogRepository;
import com.wrenchlog.wrenchlog.repository.VehicleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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
    public List<ServiceLog> getServicesForVehicle(@RequestParam Long vehicleId){
        return serviceLogRepository.findByVehicleId(vehicleId);
    }

    @PostMapping
    public ResponseEntity<ServiceLog> addServiceLog(@RequestParam Long vehicleId, @RequestBody ServiceLog serviceLog){
        return vehicleRepository.findById(vehicleId)
                .map(vehicle -> {
                    serviceLog.setVehicle(vehicle);
                    ServiceLog savedLog = serviceLogRepository.save(serviceLog);
                    return new ResponseEntity<>(savedLog, HttpStatus.CREATED);
                })
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteServiceLog(@PathVariable Long id){
        if(serviceLogRepository.existsById(id)){
            serviceLogRepository.deleteById(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }else{
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
