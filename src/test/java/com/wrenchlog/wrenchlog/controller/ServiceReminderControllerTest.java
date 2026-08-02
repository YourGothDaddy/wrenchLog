package com.wrenchlog.wrenchlog.controller;

import com.wrenchlog.wrenchlog.model.ServiceReminder;
import com.wrenchlog.wrenchlog.model.User;
import com.wrenchlog.wrenchlog.model.Vehicle;
import com.wrenchlog.wrenchlog.repository.ServiceReminderRepository;
import com.wrenchlog.wrenchlog.service.VehicleAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ServiceReminderControllerTest {

    private ServiceReminderRepository serviceReminderRepository;
    private VehicleAccessService vehicleAccessService;
    private ServiceReminderController serviceReminderController;

    @BeforeEach
    void setUp() {
        serviceReminderRepository = mock(ServiceReminderRepository.class);
        vehicleAccessService = mock(VehicleAccessService.class);
        serviceReminderController = new ServiceReminderController(vehicleAccessService, serviceReminderRepository);
    }

    @Test
    void getServiceRemindersForVehicle_returnsReminders_whenOwner() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);

        ServiceReminder reminder = new ServiceReminder();
        reminder.setId(200L);
        reminder.setTitle("Oil change");
        reminder.setVehicle(vehicle);

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, owner)).thenReturn(vehicle);
        when(serviceReminderRepository.findByVehicleId(10L)).thenReturn(List.of(reminder));

        List<ServiceReminder> response = serviceReminderController.getServiceRemindersForVehicle(10L, owner);

        assertEquals(1, response.size());
        assertEquals("Oil change", response.getFirst().getTitle());

        verify(vehicleAccessService).getOwnedVehicleOrThrow(10L, owner);
    }

    @Test
    void getServiceRemindersForVehicle_throwsForbidden_whenNotOwner() {
        User attacker = new User("bob", "bob@test.com", "hashed");
        attacker.setId(2L);

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, attacker))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));

        assertThrows(ResponseStatusException.class,
                () -> serviceReminderController.getServiceRemindersForVehicle(10L, attacker));

        verify(serviceReminderRepository, never()).findByVehicleId(any());
    }

    @Test
    void addServiceReminder_returnsCreated_withCorrectData() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);

        ServiceReminder incoming = new ServiceReminder();
        incoming.setTitle("Timing belt");

        ServiceReminder saved = new ServiceReminder();
        saved.setId(201L);
        saved.setTitle("Timing belt");
        saved.setVehicle(vehicle);

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, owner)).thenReturn(vehicle);
        when(serviceReminderRepository.save(any(ServiceReminder.class))).thenReturn(saved);

        ResponseEntity<ServiceReminder> response =
                serviceReminderController.addServiceReminder(10L, incoming, owner);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(201L, response.getBody().getId());
        assertEquals("Timing belt", response.getBody().getTitle());

        verify(serviceReminderRepository).save(any(ServiceReminder.class));
    }

    @Test
    void addServiceReminder_throwsForbidden_whenNotOwner() {
        User attacker = new User("bob", "bob@test.com", "hashed");
        attacker.setId(2L);

        ServiceReminder incoming = new ServiceReminder();

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, attacker))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));

        assertThrows(ResponseStatusException.class,
                () -> serviceReminderController.addServiceReminder(10L, incoming, attacker));

        verify(serviceReminderRepository, never()).save(any());
    }

    @Test
    void modifyServiceReminder_updatesSuccessfully_whenOwner() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);

        LocalDateTime originalCreatedAt = LocalDateTime.of(2025, 1, 1, 10, 0);

        ServiceReminder existing = new ServiceReminder();
        existing.setId(200L);
        existing.setVehicle(vehicle);
        existing.setCreatedAt(originalCreatedAt);

        ServiceReminder updatedInput = new ServiceReminder();
        updatedInput.setTitle("Updated title");

        ServiceReminder savedResult = new ServiceReminder();
        savedResult.setId(200L);
        savedResult.setTitle("Updated title");
        savedResult.setVehicle(vehicle);
        savedResult.setCreatedAt(originalCreatedAt);

        when(serviceReminderRepository.findById(200L)).thenReturn(Optional.of(existing));
        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, owner)).thenReturn(vehicle);
        when(serviceReminderRepository.save(any(ServiceReminder.class))).thenReturn(savedResult);

        ResponseEntity<ServiceReminder> response =
                serviceReminderController.modifyServiceReminder(200L, 10L, updatedInput, owner);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Updated title", response.getBody().getTitle());
        assertEquals(originalCreatedAt, response.getBody().getCreatedAt());

        verify(vehicleAccessService).assertOwnership(vehicle, owner);
    }

    @Test
    void modifyServiceReminder_throwsNotFound_whenReminderMissing() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        ServiceReminder updatedInput = new ServiceReminder();

        when(serviceReminderRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> serviceReminderController.modifyServiceReminder(999L, 10L, updatedInput, owner));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(serviceReminderRepository, never()).save(any());
    }

    @Test
    void modifyServiceReminder_throwsForbidden_whenNotOwner() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        User attacker = new User("bob", "bob@test.com", "hashed");
        attacker.setId(2L);

        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);

        ServiceReminder existing = new ServiceReminder();
        existing.setId(200L);
        existing.setVehicle(vehicle);

        ServiceReminder updatedInput = new ServiceReminder();

        when(serviceReminderRepository.findById(200L)).thenReturn(Optional.of(existing));
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN))
                .when(vehicleAccessService).assertOwnership(vehicle, attacker);

        assertThrows(ResponseStatusException.class,
                () -> serviceReminderController.modifyServiceReminder(200L, 10L, updatedInput, attacker));

        verify(serviceReminderRepository, never()).save(any());
    }

    @Test
    void deleteServiceReminder_deletesSuccessfully_whenOwner() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);

        ServiceReminder reminder = new ServiceReminder();
        reminder.setId(200L);
        reminder.setVehicle(vehicle);

        when(serviceReminderRepository.findById(200L)).thenReturn(Optional.of(reminder));

        ResponseEntity<Void> response = serviceReminderController.deleteServiceReminder(200L, owner);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(serviceReminderRepository).deleteById(200L);
    }

    @Test
    void deleteServiceReminder_throwsNotFound_whenReminderMissing() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        when(serviceReminderRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> serviceReminderController.deleteServiceReminder(999L, owner));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(serviceReminderRepository, never()).deleteById(any());
    }

    @Test
    void deleteServiceReminder_throwsForbidden_whenNotOwner() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        User attacker = new User("bob", "bob@test.com", "hashed");
        attacker.setId(2L);

        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);

        ServiceReminder reminder = new ServiceReminder();
        reminder.setId(200L);
        reminder.setVehicle(vehicle);

        when(serviceReminderRepository.findById(200L)).thenReturn(Optional.of(reminder));
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN))
                .when(vehicleAccessService).assertOwnership(vehicle, attacker);

        assertThrows(ResponseStatusException.class,
                () -> serviceReminderController.deleteServiceReminder(200L, attacker));

        verify(serviceReminderRepository, never()).deleteById(any());
    }
}