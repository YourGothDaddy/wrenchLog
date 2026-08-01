package com.wrenchlog.wrenchlog.controller;

import com.wrenchlog.wrenchlog.dto.VehicleCreateDTO;
import com.wrenchlog.wrenchlog.dto.VehicleResponseDTO;
import com.wrenchlog.wrenchlog.model.User;
import com.wrenchlog.wrenchlog.model.Vehicle;
import com.wrenchlog.wrenchlog.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VehicleControllerTest {

    private VehicleRepository vehicleRepository;
    private VehicleController vehicleController;

    @BeforeEach
    void setUp() {
        vehicleRepository = mock(VehicleRepository.class);
        vehicleController = new VehicleController(vehicleRepository);
    }

    @Test
    void deletingOwnVehicle_shouldSucceed() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);

        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));

        ResponseEntity<Void> response = vehicleController.deleteVehicleFromGarage(10L, owner);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(vehicleRepository).delete(vehicle);
    }

    @Test
    void deletingSomeoneElsesVehicle_shouldReturn403() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        User attacker = new User("bob", "bob@test.com", "hashed");
        attacker.setId(2L);

        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);

        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> vehicleController.deleteVehicleFromGarage(10L, attacker)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(vehicleRepository, never()).delete(any());
    }

    @Test
    void deletingNonexistentVehicle_shouldReturn404() {
        User user = new User("alice", "alice@test.com", "hashed");
        user.setId(1L);

        when(vehicleRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> vehicleController.deleteVehicleFromGarage(999L, user)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void getMyGarage_returnsCorrectUserCountVehicles() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);

        when(vehicleRepository.findByUserUsername(owner.getUsername())).thenReturn(List.of(vehicle));

        List<VehicleResponseDTO> response = vehicleController.getMyGarage(owner);

        assertEquals(1, response.size());

        VehicleResponseDTO dto = response.getFirst();
        assertEquals(10L, dto.getId());
        assertEquals("Toyota", dto.getMake());
        assertEquals("Corolla", dto.getModel());
        assertEquals(2020, dto.getYear());
        assertEquals(50000, dto.getKilometers());
        assertEquals("alice", dto.getUsername());
        verify(vehicleRepository).findByUserUsername(owner.getUsername());
    }

    @Test
    void getMyGarage_returnsEmptyListOfVehicle(){
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        when(vehicleRepository.findByUserUsername(owner.getUsername())).thenReturn(List.of());

        List<VehicleResponseDTO> response = vehicleController.getMyGarage(owner);

        assertEquals(0, response.size());
        verify(vehicleRepository).findByUserUsername(owner.getUsername());
    }

    @Test
    void getMyGarage_returnsAllVehiclesWithCorrectMappingWhenMultiple() {
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
    void addVehicleToGarage_returnsCreatedWithCorrectData(){
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        VehicleCreateDTO dto = new VehicleCreateDTO();
        dto.setMake("Toyota");
        dto.setModel("Corolla");
        dto.setYear(2020);
        dto.setKilometers(50000);

        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);

        when(vehicleRepository.save(any(Vehicle.class))).thenReturn(vehicle);

        ResponseEntity<VehicleResponseDTO> response = vehicleController.addVehicleToGarage(dto, owner);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(10L, response.getBody().getId());
        assertEquals("Toyota", response.getBody().getMake());
        assertEquals("Corolla", response.getBody().getModel());
        assertEquals(2020, response.getBody().getYear());
        assertEquals(50000, response.getBody().getKilometers());
        assertEquals("alice", response.getBody().getUsername());

        verify(vehicleRepository).save(any());
    }
}