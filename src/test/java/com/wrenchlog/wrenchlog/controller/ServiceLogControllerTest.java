package com.wrenchlog.wrenchlog.controller;

import com.wrenchlog.wrenchlog.dto.ServiceLogResponseDTO;
import com.wrenchlog.wrenchlog.model.ServiceLog;
import com.wrenchlog.wrenchlog.model.User;
import com.wrenchlog.wrenchlog.model.Vehicle;
import com.wrenchlog.wrenchlog.repository.ServiceLogRepository;
import com.wrenchlog.wrenchlog.service.VehicleAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ServiceLogControllerTest {

    private ServiceLogRepository serviceLogRepository;
    private VehicleAccessService vehicleAccessService;
    private ServiceLogController serviceLogController;

    @BeforeEach
    void setUp() {
        serviceLogRepository = mock(ServiceLogRepository.class);
        vehicleAccessService = mock(VehicleAccessService.class);
        serviceLogController = new ServiceLogController(serviceLogRepository, vehicleAccessService);
    }

    @Test
    void getServicesForVehicle_returnsLogsForOwnedVehicle() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);

        ServiceLog log = new ServiceLog();
        log.setId(100L);
        log.setDescription("Oil change");
        log.setCost(BigDecimal.valueOf(49.99));
        log.setKilometersAtService(51000);
        log.setServiceDate(LocalDate.of(2026, 1, 15));
        log.setVehicle(vehicle);

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, owner)).thenReturn(vehicle);
        when(serviceLogRepository.findByVehicleId(10L)).thenReturn(List.of(log));

        List<ServiceLogResponseDTO> response = serviceLogController.getServicesForVehicle(10L, owner);

        assertEquals(1, response.size());
        ServiceLogResponseDTO dto = response.getFirst();
        assertEquals(100L, dto.getId());
        assertEquals("Oil change", dto.getDescription());
        assertEquals(BigDecimal.valueOf(49.99), dto.getCost());
        assertEquals(51000, dto.getKilometersAtService());
        assertEquals(LocalDate.of(2026, 1, 15), dto.getServiceDate());
        assertEquals(10L, dto.getVehicleId());

        verify(vehicleAccessService).getOwnedVehicleOrThrow(10L, owner);
    }

    @Test
    void getServicesForVehicle_throwsForbidden_whenNotOwner() {
        User attacker = new User("bob", "bob@test.com", "hashed");
        attacker.setId(2L);

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, attacker))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));

        assertThrows(ResponseStatusException.class,
                () -> serviceLogController.getServicesForVehicle(10L, attacker));

        verify(serviceLogRepository, never()).findByVehicleId(any());
    }

    @Test
    void addServiceLog_returnsCreatedWithCorrectData() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);

        ServiceLog incoming = new ServiceLog();
        incoming.setDescription("Brake pads");
        incoming.setCost(BigDecimal.valueOf(120.00));
        incoming.setKilometersAtService(52000);
        incoming.setServiceDate(LocalDate.of(2026, 2, 1));

        ServiceLog saved = new ServiceLog();
        saved.setId(101L);
        saved.setDescription("Brake pads");
        saved.setCost(BigDecimal.valueOf(120.00));
        saved.setKilometersAtService(52000);
        saved.setServiceDate(LocalDate.of(2026, 2, 1));
        saved.setVehicle(vehicle);

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, owner)).thenReturn(vehicle);
        when(serviceLogRepository.save(any(ServiceLog.class))).thenReturn(saved);

        ResponseEntity<ServiceLogResponseDTO> response =
                serviceLogController.addServiceLog(10L, incoming, owner);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(101L, response.getBody().getId());
        assertEquals("Brake pads", response.getBody().getDescription());
        assertEquals(10L, response.getBody().getVehicleId());

        verify(serviceLogRepository).save(any(ServiceLog.class));
    }

    @Test
    void addServiceLog_throwsForbidden_whenNotOwner() {
        User attacker = new User("bob", "bob@test.com", "hashed");
        attacker.setId(2L);

        ServiceLog incoming = new ServiceLog();

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, attacker))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));

        assertThrows(ResponseStatusException.class,
                () -> serviceLogController.addServiceLog(10L, incoming, attacker));

        verify(serviceLogRepository, never()).save(any());
    }

    @Test
    void modifyServiceLog_updatesSuccessfully_whenOwner() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);

        ServiceLog existing = new ServiceLog();
        existing.setId(100L);
        existing.setVehicle(vehicle);

        ServiceLog updatedInput = new ServiceLog();
        updatedInput.setDescription("Updated description");
        updatedInput.setCost(BigDecimal.valueOf(75.00));
        updatedInput.setKilometersAtService(53000);
        updatedInput.setServiceDate(LocalDate.of(2026, 3, 1));

        ServiceLog savedResult = new ServiceLog();
        savedResult.setId(100L);
        savedResult.setDescription("Updated description");
        savedResult.setCost(BigDecimal.valueOf(75.00));
        savedResult.setKilometersAtService(53000);
        savedResult.setServiceDate(LocalDate.of(2026, 3, 1));
        savedResult.setVehicle(vehicle);

        when(serviceLogRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, owner)).thenReturn(vehicle);
        when(serviceLogRepository.save(any(ServiceLog.class))).thenReturn(savedResult);

        ResponseEntity<ServiceLogResponseDTO> response =
                serviceLogController.modifyServiceLog(100L, 10L, updatedInput, owner);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Updated description", response.getBody().getDescription());
        assertEquals(BigDecimal.valueOf(75.00), response.getBody().getCost());

        verify(vehicleAccessService).assertOwnership(vehicle, owner);
    }

    @Test
    void modifyServiceLog_throws404_whenLogNotFound() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        ServiceLog updatedInput = new ServiceLog();

        when(serviceLogRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> serviceLogController.modifyServiceLog(999L, 10L, updatedInput, owner));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(serviceLogRepository, never()).save(any());
    }

    @Test
    void modifyServiceLog_throwsForbidden_whenNotOwner() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        User attacker = new User("bob", "bob@test.com", "hashed");
        attacker.setId(2L);

        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);

        ServiceLog existing = new ServiceLog();
        existing.setId(100L);
        existing.setVehicle(vehicle);

        ServiceLog updatedInput = new ServiceLog();

        when(serviceLogRepository.findById(100L)).thenReturn(Optional.of(existing));
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN))
                .when(vehicleAccessService).assertOwnership(vehicle, attacker);

        assertThrows(ResponseStatusException.class,
                () -> serviceLogController.modifyServiceLog(100L, 10L, updatedInput, attacker));

        verify(serviceLogRepository, never()).save(any());
    }

    @Test
    void deleteServiceLog_deletesSuccessfully_whenOwner() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);

        ServiceLog log = new ServiceLog();
        log.setId(100L);
        log.setVehicle(vehicle);

        when(serviceLogRepository.findById(100L)).thenReturn(Optional.of(log));

        ResponseEntity<Void> response = serviceLogController.deleteServiceLog(100L, owner);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(serviceLogRepository).deleteById(100L);
    }

    @Test
    void deleteServiceLog_throws404_whenLogNotFound() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        when(serviceLogRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> serviceLogController.deleteServiceLog(999L, owner));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(serviceLogRepository, never()).deleteById(any());
    }

    @Test
    void deleteServiceLog_throwsForbidden_whenNotOwner() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        User attacker = new User("bob", "bob@test.com", "hashed");
        attacker.setId(2L);

        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);

        ServiceLog log = new ServiceLog();
        log.setId(100L);
        log.setVehicle(vehicle);

        when(serviceLogRepository.findById(100L)).thenReturn(Optional.of(log));
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN))
                .when(vehicleAccessService).assertOwnership(vehicle, attacker);

        assertThrows(ResponseStatusException.class,
                () -> serviceLogController.deleteServiceLog(100L, attacker));

        verify(serviceLogRepository, never()).deleteById(any());
    }
}