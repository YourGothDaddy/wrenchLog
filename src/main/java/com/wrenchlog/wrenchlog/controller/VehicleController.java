package com.wrenchlog.wrenchlog.controller;

import com.wrenchlog.wrenchlog.dto.VehicleCreateDTO;
import com.wrenchlog.wrenchlog.dto.VehicleResponseDTO;
import com.wrenchlog.wrenchlog.model.User;
import com.wrenchlog.wrenchlog.model.Vehicle;
import com.wrenchlog.wrenchlog.repository.UserRepository;
import com.wrenchlog.wrenchlog.repository.VehicleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {
    private final VehicleRepository vehicleRepository;

    public VehicleController(VehicleRepository vehicleRepository){
        this.vehicleRepository = vehicleRepository;
    }

    @GetMapping
    public List<VehicleResponseDTO> getMyGarage(@AuthenticationPrincipal User user){
        List<Vehicle> vehicles = vehicleRepository.findByUserUsername(user.getUsername());

        return vehicles.stream()
                .map(vehicle -> new VehicleResponseDTO(
                        vehicle.getId(),
                        vehicle.getMake(),
                        vehicle.getModel(),
                        vehicle.getYear(),
                        vehicle.getKilometers(),
                        vehicle.getUser().getUsername()
                ))
                .toList();
    }

    @PostMapping
    public ResponseEntity<VehicleResponseDTO> addVehicleToGarage(@RequestBody VehicleCreateDTO dto,
                                                                 @AuthenticationPrincipal User user){

        Vehicle vehicle = new Vehicle(
                dto.getMake(),
                dto.getModel(),
                dto.getYear(),
                dto.getKilometers(),
                user
        );

        Vehicle savedVehicle = vehicleRepository.save(vehicle);

        VehicleResponseDTO responseBody = new VehicleResponseDTO(
                savedVehicle.getId(),
                savedVehicle.getMake(),
                savedVehicle.getModel(),
                savedVehicle.getYear(),
                savedVehicle.getKilometers(),
                user.getUsername()
        );
        return new ResponseEntity<>(responseBody, HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVehicleFromGarage(@PathVariable Long id,
                                                        @AuthenticationPrincipal User user){

        Vehicle vehicle  = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehicle not found"));

        if(!vehicle.getUser().getId().equals(user.getId())){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this vehicle");
        }

        vehicleRepository.delete(vehicle);
        return ResponseEntity.noContent().build();
    }
}
