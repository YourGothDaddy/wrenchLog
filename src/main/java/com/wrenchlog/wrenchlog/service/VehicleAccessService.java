package com.wrenchlog.wrenchlog.service;

import com.wrenchlog.wrenchlog.model.User;
import com.wrenchlog.wrenchlog.model.Vehicle;
import com.wrenchlog.wrenchlog.repository.VehicleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class VehicleAccessService {

    private final VehicleRepository vehicleRepository;

    public VehicleAccessService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    public Vehicle getOwnedVehicleOrThrow(Long vehicleId,
                                          User user) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehicle not found"));

        assertOwnership(vehicle, user);
        return vehicle;
    }

    public void assertOwnership(Vehicle vehicle,
                                User user) {
        if (!vehicle.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this vehicle");
        }
    }
}