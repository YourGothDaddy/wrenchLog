package com.wrenchlog.wrenchlog.controller;

import com.wrenchlog.wrenchlog.dto.ServiceLogCreateDTO;
import com.wrenchlog.wrenchlog.dto.ServiceLogResponseDTO;
import com.wrenchlog.wrenchlog.model.ServiceLog;
import com.wrenchlog.wrenchlog.model.User;
import com.wrenchlog.wrenchlog.model.Vehicle;
import com.wrenchlog.wrenchlog.repository.ServiceLogRepository;
import com.wrenchlog.wrenchlog.repository.VehicleRepository;
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
    private VehicleRepository vehicleRepository;

    @BeforeEach
    void setUp() {
        serviceLogRepository = mock(ServiceLogRepository.class);
        vehicleAccessService = mock(VehicleAccessService.class);
        vehicleRepository = mock(VehicleRepository.class);
        serviceLogController = new ServiceLogController(serviceLogRepository, vehicleAccessService, vehicleRepository);
    }

    @Test
    void getServicesForVehicle_returnsLogs_whenOwner() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);

        ServiceLog log = new ServiceLog("Oil change", new BigDecimal("49.99"), 51000, LocalDate.of(2026, 1, 15), vehicle);
        log.setId(100L);

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, owner)).thenReturn(vehicle);
        when(serviceLogRepository.findByVehicleId(10L)).thenReturn(List.of(log));

        List<ServiceLogResponseDTO> response = serviceLogController.getServicesForVehicle(10L, owner);

        assertEquals(1, response.size());
        ServiceLogResponseDTO dto = response.get(0);
        assertEquals(100L, dto.id());
        assertEquals("Oil change", dto.description());
        assertEquals(10L, dto.vehicleId());

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
    void addServiceLog_returnsCreated_withCorrectData() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);

        ServiceLogCreateDTO dto = new ServiceLogCreateDTO("Brake pads", new BigDecimal("120.00"), 52000, LocalDate.of(2026, 2, 1));

        ServiceLog saved = new ServiceLog("Brake pads", new BigDecimal("120.00"), 52000, LocalDate.of(2026, 2, 1), vehicle);
        saved.setId(101L);

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, owner)).thenReturn(vehicle);
        when(serviceLogRepository.save(any(ServiceLog.class))).thenReturn(saved);

        ResponseEntity<ServiceLogResponseDTO> response =
                serviceLogController.addServiceLog(10L, dto, owner);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(101L, response.getBody().id());
        assertEquals("Brake pads", response.getBody().description());
        assertEquals(10L, response.getBody().vehicleId());

        verify(serviceLogRepository).save(any(ServiceLog.class));
    }

    @Test
    void addServiceLog_throwsForbidden_whenNotOwner() {
        User attacker = new User("bob", "bob@test.com", "hashed");
        attacker.setId(2L);

        ServiceLogCreateDTO dto = new ServiceLogCreateDTO("Brake pads", new BigDecimal("120.00"), 52000, LocalDate.of(2026, 2, 1));

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, attacker))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));

        assertThrows(ResponseStatusException.class,
                () -> serviceLogController.addServiceLog(10L, dto, attacker));

        verify(serviceLogRepository, never()).save(any());
    }

    @Test
    void modifyServiceLog_updatesSuccessfully_whenOwner() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);

        ServiceLog existing = new ServiceLog("Old description", new BigDecimal("10.00"), 50000, LocalDate.of(2026, 1, 1), vehicle);
        existing.setId(100L);

        ServiceLogCreateDTO dto = new ServiceLogCreateDTO("Updated description", new BigDecimal("75.00"), 53000, LocalDate.of(2026, 3, 1));

        when(serviceLogRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, owner)).thenReturn(vehicle);
        when(serviceLogRepository.save(any(ServiceLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<ServiceLogResponseDTO> response =
                serviceLogController.modifyServiceLog(100L, 10L, dto, owner);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Updated description", response.getBody().description());
        assertEquals(new BigDecimal("75.00"), response.getBody().cost());

        verify(vehicleAccessService).assertOwnership(vehicle, owner);
    }

    @Test
    void modifyServiceLog_throwsNotFound_whenLogMissing() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        ServiceLogCreateDTO dto = new ServiceLogCreateDTO("Title", new BigDecimal("10.00"), 1000, LocalDate.now());

        when(serviceLogRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> serviceLogController.modifyServiceLog(999L, 10L, dto, owner));

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

        ServiceLog existing = new ServiceLog("Title", new BigDecimal("10.00"), 1000, LocalDate.now(), vehicle);
        existing.setId(100L);

        ServiceLogCreateDTO dto = new ServiceLogCreateDTO("Title", new BigDecimal("10.00"), 1000, LocalDate.now());

        when(serviceLogRepository.findById(100L)).thenReturn(Optional.of(existing));
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN))
                .when(vehicleAccessService).assertOwnership(vehicle, attacker);

        assertThrows(ResponseStatusException.class,
                () -> serviceLogController.modifyServiceLog(100L, 10L, dto, attacker));

        verify(serviceLogRepository, never()).save(any());
    }

    @Test
    void deleteServiceLog_deletesSuccessfully_whenOwner() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);

        ServiceLog log = new ServiceLog("Title", new BigDecimal("10.00"), 1000, LocalDate.now(), vehicle);
        log.setId(100L);

        when(serviceLogRepository.findById(100L)).thenReturn(Optional.of(log));

        ResponseEntity<Void> response = serviceLogController.deleteServiceLog(100L, owner);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(serviceLogRepository).deleteById(100L);
    }

    @Test
    void deleteServiceLog_throwsNotFound_whenLogMissing() {
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

        ServiceLog log = new ServiceLog("Title", new BigDecimal("10.00"), 1000, LocalDate.now(), vehicle);
        log.setId(100L);

        when(serviceLogRepository.findById(100L)).thenReturn(Optional.of(log));
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN))
                .when(vehicleAccessService).assertOwnership(vehicle, attacker);

        assertThrows(ResponseStatusException.class,
                () -> serviceLogController.deleteServiceLog(100L, attacker));

        verify(serviceLogRepository, never()).deleteById(any());
    }

    @Test
    void addServiceLog_advancesOdometer_whenKilometersHigherThanCurrent() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 350000, owner);
        vehicle.setId(10L);

        ServiceLogCreateDTO dto = new ServiceLogCreateDTO("Oil change", new BigDecimal("50.00"), 355000, LocalDate.now());

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, owner)).thenReturn(vehicle);
        when(serviceLogRepository.save(any(ServiceLog.class))).thenAnswer(inv -> inv.getArgument(0));

        serviceLogController.addServiceLog(10L, dto, owner);

        assertEquals(355000, vehicle.getKilometers());
        verify(vehicleRepository).save(vehicle);
    }

    @Test
    void addServiceLog_doesNotChangeOdometer_whenKilometersLowerThanCurrent() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 350000, owner);
        vehicle.setId(10L);

        ServiceLogCreateDTO dto = new ServiceLogCreateDTO("Old repair", new BigDecimal("20.00"), 300000, LocalDate.now());

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, owner)).thenReturn(vehicle);
        when(serviceLogRepository.save(any(ServiceLog.class))).thenAnswer(inv -> inv.getArgument(0));

        serviceLogController.addServiceLog(10L, dto, owner);

        assertEquals(350000, vehicle.getKilometers());
        verify(vehicleRepository, never()).save(any());
    }
}