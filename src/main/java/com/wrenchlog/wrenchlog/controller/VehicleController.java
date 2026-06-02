package com.wrenchlog.wrenchlog.controller;

import com.wrenchlog.wrenchlog.model.Vehicle;
import com.wrenchlog.wrenchlog.repository.VehicleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {
    private final VehicleRepository vehicleRepository;

    public VehicleController(VehicleRepository vehicleRepository){
        this.vehicleRepository = vehicleRepository;
    }

    @GetMapping
    public List<Vehicle> getMyGarage(@RequestParam String userId){
        return vehicleRepository.findByUserId(userId);
    }

    @PostMapping
    public ResponseEntity<Vehicle> addVehicleToGarage(@RequestBody Vehicle vehicle){
        Vehicle savedVehicle = vehicleRepository.save(vehicle);
        return new ResponseEntity<>(savedVehicle, HttpStatus.CREATED);
    }
}
