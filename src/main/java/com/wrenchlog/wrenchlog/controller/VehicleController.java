package com.wrenchlog.wrenchlog.controller;

import com.wrenchlog.wrenchlog.dto.VehicleCreateDTO;
import com.wrenchlog.wrenchlog.dto.VehicleDetailsUpdateDTO;
import com.wrenchlog.wrenchlog.dto.VehicleResponseDTO;
import com.wrenchlog.wrenchlog.enums.DriveType;
import com.wrenchlog.wrenchlog.enums.FuelType;
import com.wrenchlog.wrenchlog.enums.TransmissionType;
import com.wrenchlog.wrenchlog.model.ServiceReminder;
import com.wrenchlog.wrenchlog.model.User;
import com.wrenchlog.wrenchlog.model.Vehicle;
import com.wrenchlog.wrenchlog.repository.ServiceReminderRepository;
import com.wrenchlog.wrenchlog.repository.VehicleRepository;
import com.wrenchlog.wrenchlog.service.VehicleAccessService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {
    private final VehicleRepository vehicleRepository;
    private final VehicleAccessService vehicleAccessService;
    private final ServiceReminderRepository serviceReminderRepository;

    public VehicleController(VehicleRepository vehicleRepository, VehicleAccessService vehicleAccessService,
                             ServiceReminderRepository serviceReminderRepository){
        this.vehicleRepository = vehicleRepository;
        this.vehicleAccessService = vehicleAccessService;
        this.serviceReminderRepository = serviceReminderRepository;
    }

    private VehicleResponseDTO toResponseDTO(Vehicle vehicle) {
        return new VehicleResponseDTO(
                vehicle.getId(), vehicle.getMake(), vehicle.getModel(), vehicle.getYear(),
                vehicle.getKilometers(), vehicle.getUser().getUsername(),
                vehicle.getVin(), vehicle.getPlateNumber(), vehicle.getEngineCode(),
                vehicle.getTransmissionType() != null ? vehicle.getTransmissionType().name() : null,
                vehicle.getDriveType() != null ? vehicle.getDriveType().name() : null,
                vehicle.getColor(),
                vehicle.getFuelType() != null ? vehicle.getFuelType().name() : null,
                vehicle.getFuelTankCapacityLiters(), vehicle.getEngineOilCapacityLiters(),
                vehicle.getEngineOilType(), vehicle.getTireSize(),
                vehicle.getPurchaseDate(), vehicle.getPurchasePrice()
        );
    }

    @GetMapping
    public List<VehicleResponseDTO> getMyGarage(@AuthenticationPrincipal User user){
        List<Vehicle> vehicles = vehicleRepository.findByUserUsername(user.getUsername());
        return vehicles.stream().map(this::toResponseDTO).toList();
    }

    @PostMapping
    public ResponseEntity<VehicleResponseDTO> addVehicleToGarage(@Valid @RequestBody VehicleCreateDTO dto,
                                                                 @AuthenticationPrincipal User user){
        Vehicle vehicle = new Vehicle(dto.make(), dto.model(), dto.year(), dto.kilometers(), user);
        Vehicle savedVehicle = vehicleRepository.save(vehicle);
        return new ResponseEntity<>(toResponseDTO(savedVehicle), HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVehicleFromGarage(@PathVariable Long id,
                                                        @AuthenticationPrincipal User user){
        Vehicle vehicle = vehicleAccessService.getOwnedVehicleOrThrow(id, user);
        vehicleRepository.delete(vehicle);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/details")
    public ResponseEntity<VehicleResponseDTO> updateVehicleDetails(
            @PathVariable Long id,
            @Valid @RequestBody VehicleDetailsUpdateDTO dto,
            @AuthenticationPrincipal User user
    ) {
        Vehicle vehicle = vehicleAccessService.getOwnedVehicleOrThrow(id, user);

        vehicle.setVin(dto.vin());
        vehicle.setPlateNumber(dto.plateNumber());
        vehicle.setEngineCode(dto.engineCode());
        vehicle.setColor(dto.color());
        vehicle.setFuelTankCapacityLiters(dto.fuelTankCapacityLiters());
        vehicle.setEngineOilCapacityLiters(dto.engineOilCapacityLiters());
        vehicle.setEngineOilType(dto.engineOilType());
        vehicle.setTireSize(dto.tireSize());
        vehicle.setPurchaseDate(dto.purchaseDate());
        vehicle.setPurchasePrice(dto.purchasePrice());

        if (dto.transmissionType() != null) {
            try {
                vehicle.setTransmissionType(TransmissionType.valueOf(dto.transmissionType()));
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid transmission type");
            }
        }
        if (dto.driveType() != null) {
            try {
                vehicle.setDriveType(DriveType.valueOf(dto.driveType()));
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid drive type");
            }
        }
        if (dto.fuelType() != null) {
            try {
                vehicle.setFuelType(FuelType.valueOf(dto.fuelType()));
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid fuel type");
            }
        }

        Vehicle saved = vehicleRepository.save(vehicle);

        createOrUpdateDateReminder(saved, "Insurance renewal", dto.insuranceExpiryDate());
        createOrUpdateDateReminder(saved, "Vignette renewal", dto.vignetteExpiryDate());
        createOrUpdateDateReminder(saved, "Inspection due", dto.inspectionDueDate());

        return ResponseEntity.ok(toResponseDTO(saved));
    }

    private void createOrUpdateDateReminder(Vehicle vehicle, String title, LocalDate dueDate) {
        if (dueDate == null) return;

        ServiceReminder existing = serviceReminderRepository.findByVehicleId(vehicle.getId())
                .stream()
                .filter(r -> title.equals(r.getTitle()))
                .findFirst()
                .orElse(null);

        if (existing != null) {
            existing.setLastServiceAtDate(dueDate.minusYears(1));
            existing.setIntervalMonths(12);
            serviceReminderRepository.save(existing);
        } else {
            ServiceReminder reminder = new ServiceReminder(
                    title, null, null, null, 12, dueDate.minusYears(1), vehicle
            );
            serviceReminderRepository.save(reminder);
        }
    }
}