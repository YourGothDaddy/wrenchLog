package com.wrenchlog.wrenchlog.controller;

import com.wrenchlog.wrenchlog.dto.VehicleCreateDTO;
import com.wrenchlog.wrenchlog.dto.VehicleResponseDTO;
import com.wrenchlog.wrenchlog.model.User;
import com.wrenchlog.wrenchlog.model.Vehicle;
import com.wrenchlog.wrenchlog.repository.UserRepository;
import com.wrenchlog.wrenchlog.repository.VehicleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;

    public VehicleController(VehicleRepository vehicleRepository, UserRepository userRepository){
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<VehicleResponseDTO> getMyGarage(@RequestParam String username){
        List<Vehicle> vehicles = vehicleRepository.findByUserUsername(username);

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
    public ResponseEntity<VehicleResponseDTO> addVehicleToGarage(@RequestBody VehicleCreateDTO dto){
        User user = userRepository.findByUsername(dto.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

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
    public ResponseEntity<Void> deleteVehicleFromGarage(@PathVariable Long id){
        if(vehicleRepository.existsById(id)){
            vehicleRepository.deleteById(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }else{
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
