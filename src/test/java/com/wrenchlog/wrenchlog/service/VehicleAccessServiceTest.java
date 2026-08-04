package com.wrenchlog.wrenchlog.service;

import com.wrenchlog.wrenchlog.model.User;
import com.wrenchlog.wrenchlog.model.Vehicle;
import com.wrenchlog.wrenchlog.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VehicleAccessServiceTest {

    private VehicleRepository vehicleRepository;
    private VehicleAccessService vehicleAccessService;

    @BeforeEach
    void setUp() {
        vehicleRepository = mock(VehicleRepository.class);
        vehicleAccessService = new VehicleAccessService(vehicleRepository);
    }

    @Test
    void getOwnedVehicleOrThrow_returnsVehicle_whenOwner() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);

        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));

        Vehicle result = vehicleAccessService.getOwnedVehicleOrThrow(10L, owner);

        assertEquals(vehicle, result);
    }

    @Test
    void getOwnedVehicleOrThrow_throwsNotFound_whenVehicleMissing() {
        User user = new User("alice", "alice@test.com", "hashed");
        user.setId(1L);

        when(vehicleRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> vehicleAccessService.getOwnedVehicleOrThrow(999L, user));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void getOwnedVehicleOrThrow_throwsForbidden_whenNotOwner() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        User attacker = new User("bob", "bob@test.com", "hashed");
        attacker.setId(2L);

        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);

        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> vehicleAccessService.getOwnedVehicleOrThrow(10L, attacker));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void assertOwnership_doesNotThrow_whenOwner() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);

        assertDoesNotThrow(() -> vehicleAccessService.assertOwnership(vehicle, owner));
    }

    @Test
    void assertOwnership_throwsForbidden_whenNotOwner() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        User attacker = new User("bob", "bob@test.com", "hashed");
        attacker.setId(2L);

        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> vehicleAccessService.assertOwnership(vehicle, attacker));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }
}