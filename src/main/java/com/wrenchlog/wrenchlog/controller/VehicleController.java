package com.wrenchlog.wrenchlog.controller;

import com.wrenchlog.wrenchlog.dto.VehicleCreateDTO;
import com.wrenchlog.wrenchlog.dto.VehicleResponseDTO;
import com.wrenchlog.wrenchlog.model.User;
import com.wrenchlog.wrenchlog.model.Vehicle;
import com.wrenchlog.wrenchlog.repository.VehicleRepository;
import com.wrenchlog.wrenchlog.service.VehicleAccessService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {
    private final VehicleRepository vehicleRepository;
    private final VehicleAccessService vehicleAccessService;

    public VehicleController(VehicleRepository vehicleRepository, VehicleAccessService vehicleAccessService){
        this.vehicleRepository = vehicleRepository;
        this.vehicleAccessService = vehicleAccessService;
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
    public ResponseEntity<VehicleResponseDTO> addVehicleToGarage(@Valid @RequestBody VehicleCreateDTO dto,
                                                                 @AuthenticationPrincipal User user){

        Vehicle vehicle = new Vehicle(
                dto.make(),
                dto.model(),
                dto.year(),
                dto.kilometers(),
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
        Vehicle vehicle = vehicleAccessService.getOwnedVehicleOrThrow(id, user);
        vehicleRepository.delete(vehicle);
        return ResponseEntity.noContent().build();
    }
}