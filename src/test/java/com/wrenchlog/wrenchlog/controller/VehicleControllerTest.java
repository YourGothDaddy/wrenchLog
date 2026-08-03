package com.wrenchlog.wrenchlog.controller;

import com.wrenchlog.wrenchlog.dto.VehicleCreateDTO;
import com.wrenchlog.wrenchlog.dto.VehicleResponseDTO;
import com.wrenchlog.wrenchlog.model.User;
import com.wrenchlog.wrenchlog.model.Vehicle;
import com.wrenchlog.wrenchlog.repository.VehicleRepository;
import com.wrenchlog.wrenchlog.service.VehicleAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VehicleControllerTest {

    private VehicleRepository vehicleRepository;
    private VehicleAccessService vehicleAccessService;
    private VehicleController vehicleController;

    @BeforeEach
    void setUp() {
        vehicleRepository = mock(VehicleRepository.class);
        vehicleAccessService = mock(VehicleAccessService.class);
        vehicleController = new VehicleController(vehicleRepository, vehicleAccessService);
    }

    @Test
    void getMyGarage_returnsAllVehicles_whenUserHasVehicles() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);

        when(vehicleRepository.findByUserUsername(owner.getUsername())).thenReturn(List.of(vehicle));

        List<VehicleResponseDTO> response = vehicleController.getMyGarage(owner);

        assertEquals(1, response.size());
        VehicleResponseDTO dto = response.get(0);
        assertEquals(10L, dto.getId());
        assertEquals("Toyota", dto.getMake());
        assertEquals("alice", dto.getUsername());

        verify(vehicleRepository).findByUserUsername(owner.getUsername());
    }

    @Test
    void getMyGarage_returnsAllVehiclesWithCorrectMapping_whenMultiple() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        Vehicle vehicle1 = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle1.setId(10L);
        Vehicle vehicle2 = new Vehicle("Honda", "Civic", 2018, 80000, owner);
        vehicle2.setId(11L);

        when(vehicleRepository.findByUserUsername(owner.getUsername()))
                .thenReturn(List.of(vehicle1, vehicle2));

        List<VehicleResponseDTO> response = vehicleController.getMyGarage(owner);

        assertEquals(2, response.size());
        assertEquals("Toyota", response.get(0).getMake());
        assertEquals("Honda", response.get(1).getMake());
    }

    @Test
    void getMyGarage_returnsEmptyList_whenNoVehicles() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        when(vehicleRepository.findByUserUsername(owner.getUsername())).thenReturn(List.of());

        List<VehicleResponseDTO> response = vehicleController.getMyGarage(owner);

        assertEquals(0, response.size());
    }

    @Test
    void addVehicleToGarage_returnsCreated_withCorrectData() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        VehicleCreateDTO dto = new VehicleCreateDTO("Toyota", "Corolla", 2020, 50000);

        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);

        when(vehicleRepository.save(any(Vehicle.class))).thenReturn(vehicle);

        ResponseEntity<VehicleResponseDTO> response = vehicleController.addVehicleToGarage(dto, owner);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(10L, response.getBody().getId());
        assertEquals("Toyota", response.getBody().getMake());
        assertEquals("alice", response.getBody().getUsername());

        verify(vehicleRepository).save(any());
    }

    @Test
    void deleteVehicleFromGarage_succeeds_whenOwner() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, owner)).thenReturn(vehicle);

        ResponseEntity<Void> response = vehicleController.deleteVehicleFromGarage(10L, owner);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(vehicleRepository).delete(vehicle);
    }

    @Test
    void deleteVehicleFromGarage_throwsForbidden_whenNotOwner() {
        User attacker = new User("bob", "bob@test.com", "hashed");
        attacker.setId(2L);

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, attacker))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));

        assertThrows(ResponseStatusException.class,
                () -> vehicleController.deleteVehicleFromGarage(10L, attacker));

        verify(vehicleRepository, never()).delete(any());
    }

    @Test
    void deleteVehicleFromGarage_throwsNotFound_whenVehicleMissing() {
        User user = new User("alice", "alice@test.com", "hashed");
        user.setId(1L);

        when(vehicleAccessService.getOwnedVehicleOrThrow(999L, user))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        assertThrows(ResponseStatusException.class,
                () -> vehicleController.deleteVehicleFromGarage(999L, user));
    }
}