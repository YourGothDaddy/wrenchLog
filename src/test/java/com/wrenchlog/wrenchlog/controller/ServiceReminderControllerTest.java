package com.wrenchlog.wrenchlog.controller;

import com.wrenchlog.wrenchlog.dto.ServiceReminderCreateDTO;
import com.wrenchlog.wrenchlog.dto.ServiceReminderResponseDTO;
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

        ServiceReminder reminder = new ServiceReminder(
                "Oil change", null, null, null, null, null, vehicle);
        reminder.setId(200L);

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, owner)).thenReturn(vehicle);
        when(serviceReminderRepository.findByVehicleId(10L)).thenReturn(List.of(reminder));

        List<ServiceReminderResponseDTO> response = serviceReminderController.getServiceRemindersForVehicle(10L, owner);

        assertEquals(1, response.size());
        assertEquals("Oil change", response.get(0).title());

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

        ServiceReminderCreateDTO dto = new ServiceReminderCreateDTO(
                "Timing belt", "Replace every 100k km", null, 10000, 12, null);

        ServiceReminder saved = new ServiceReminder(
                "Timing belt", "Replace every 100k km", null, 10000, 12, null, vehicle);
        saved.setId(201L);

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, owner)).thenReturn(vehicle);
        when(serviceReminderRepository.save(any(ServiceReminder.class))).thenReturn(saved);

        ResponseEntity<ServiceReminderResponseDTO> response =
                serviceReminderController.addServiceReminder(10L, dto, owner);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(201L, response.getBody().id());
        assertEquals("Timing belt", response.getBody().title());

        verify(serviceReminderRepository).save(any(ServiceReminder.class));
    }

    @Test
    void addServiceReminder_throwsForbidden_whenNotOwner() {
        User attacker = new User("bob", "bob@test.com", "hashed");
        attacker.setId(2L);

        ServiceReminderCreateDTO dto = new ServiceReminderCreateDTO("Title", null, null, null, null, null);

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, attacker))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));

        assertThrows(ResponseStatusException.class,
                () -> serviceReminderController.addServiceReminder(10L, dto, attacker));

        verify(serviceReminderRepository, never()).save(any());
    }

    @Test
    void modifyServiceReminder_updatesSuccessfully_whenOwner_andPreservesCreatedAt() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);

        LocalDateTime originalCreatedAt = LocalDateTime.of(2025, 1, 1, 10, 0);

        ServiceReminder existing = new ServiceReminder(
                "Old title", null, null, null, null, null, vehicle);
        existing.setId(200L);
        existing.setCreatedAt(originalCreatedAt);

        ServiceReminderCreateDTO dto = new ServiceReminderCreateDTO("Updated title", null, null, null, null, null);

        when(serviceReminderRepository.findById(200L)).thenReturn(Optional.of(existing));
        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, owner)).thenReturn(vehicle);
        when(serviceReminderRepository.save(any(ServiceReminder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<ServiceReminderResponseDTO> response =
                serviceReminderController.modifyServiceReminder(200L, 10L, dto, owner);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Updated title", response.getBody().title());
        assertEquals(originalCreatedAt, response.getBody().createdAt());

        verify(vehicleAccessService).assertOwnership(vehicle, owner);
    }

    @Test
    void modifyServiceReminder_throwsNotFound_whenReminderMissing() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        ServiceReminderCreateDTO dto = new ServiceReminderCreateDTO("Title", null, null, null, null, null);

        when(serviceReminderRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> serviceReminderController.modifyServiceReminder(999L, 10L, dto, owner));

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

        ServiceReminder existing = new ServiceReminder(
                "Title", null, null, null, null, null, vehicle);
        existing.setId(200L);

        ServiceReminderCreateDTO dto = new ServiceReminderCreateDTO("Title", null, null, null, null, null);

        when(serviceReminderRepository.findById(200L)).thenReturn(Optional.of(existing));
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN))
                .when(vehicleAccessService).assertOwnership(vehicle, attacker);

        assertThrows(ResponseStatusException.class,
                () -> serviceReminderController.modifyServiceReminder(200L, 10L, dto, attacker));

        verify(serviceReminderRepository, never()).save(any());
    }

    @Test
    void deleteServiceReminder_deletesSuccessfully_whenOwner() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);

        ServiceReminder reminder = new ServiceReminder(
                "Title", null, null, null, null, null, vehicle);
        reminder.setId(200L);

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

        ServiceReminder reminder = new ServiceReminder(
                "Title", null, null, null, null, null, vehicle);
        reminder.setId(200L);

        when(serviceReminderRepository.findById(200L)).thenReturn(Optional.of(reminder));
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN))
                .when(vehicleAccessService).assertOwnership(vehicle, attacker);

        assertThrows(ResponseStatusException.class,
                () -> serviceReminderController.deleteServiceReminder(200L, attacker));

        verify(serviceReminderRepository, never()).deleteById(any());
    }
}