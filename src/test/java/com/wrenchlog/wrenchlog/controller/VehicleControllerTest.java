package com.wrenchlog.wrenchlog.controller;

import com.wrenchlog.wrenchlog.dto.BgTollVignetteDTO;
import com.wrenchlog.wrenchlog.dto.VehicleCreateDTO;
import com.wrenchlog.wrenchlog.dto.VehicleResponseDTO;
import com.wrenchlog.wrenchlog.dto.VignetteCheckResponseDTO;
import com.wrenchlog.wrenchlog.model.ServiceReminder;
import com.wrenchlog.wrenchlog.model.User;
import com.wrenchlog.wrenchlog.model.Vehicle;
import com.wrenchlog.wrenchlog.repository.ServiceReminderRepository;
import com.wrenchlog.wrenchlog.repository.VehicleRepository;
import com.wrenchlog.wrenchlog.service.BgTollService;
import com.wrenchlog.wrenchlog.service.VehicleAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VehicleControllerTest {

    private VehicleRepository vehicleRepository;
    private VehicleAccessService vehicleAccessService;
    private VehicleController vehicleController;
    private ServiceReminderRepository serviceReminderRepository;
    private BgTollService bgTollService;

    @BeforeEach
    void setUp() {
        vehicleRepository = mock(VehicleRepository.class);
        vehicleAccessService = mock(VehicleAccessService.class);
        serviceReminderRepository = mock(ServiceReminderRepository.class);
        bgTollService = mock(BgTollService.class);
        vehicleController = new VehicleController(
                vehicleRepository,
                vehicleAccessService,
                serviceReminderRepository,
                bgTollService);
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
        VehicleResponseDTO dto = response.getFirst();
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

    @Test
    void checkVignette_returnsNotApplicable_whenNoPlateNumber() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, owner)).thenReturn(vehicle);

        VignetteCheckResponseDTO result = vehicleController.checkVignette(10L, owner);

        assertFalse(result.hasLocalReminder());
        assertFalse(result.bgTollFound());
        assertEquals("No plate number on file", result.message());
        verifyNoInteractions(bgTollService);
    }

    @Test
    void checkVignette_returnsMatch_whenLocalReminderMatchesBgToll() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);
        vehicle.setPlateNumber("CA1234BC");

        ServiceReminder reminder = new ServiceReminder(
                "Vignette renewal", null, null, null, 12, LocalDate.of(2025, 12, 31), vehicle);

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, owner)).thenReturn(vehicle);
        when(serviceReminderRepository.findByVehicleId(10L)).thenReturn(List.of(reminder));

        BgTollVignetteDTO bgTollVignette = new BgTollVignetteDTO(
                "CA1234BC", LocalDateTime.of(2026, 12, 31, 0, 0), "Активна", true);
        when(bgTollService.lookupVignette("CA1234BC")).thenReturn(Optional.of(bgTollVignette));

        VignetteCheckResponseDTO result = vehicleController.checkVignette(10L, owner);

        assertTrue(result.hasLocalReminder());
        assertTrue(result.bgTollFound());
        assertTrue(result.match());
        assertEquals(LocalDate.of(2026, 12, 31), result.enteredExpiryDate());
        assertEquals("Confirmed by BGTOLL", result.message());
    }

    @Test
    void checkVignette_returnsMismatch_whenDatesDiffer() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);
        vehicle.setPlateNumber("CA1234BC");

        ServiceReminder reminder = new ServiceReminder(
                "Vignette renewal", null, null, null, 12, LocalDate.of(2025, 6, 1), vehicle);

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, owner)).thenReturn(vehicle);
        when(serviceReminderRepository.findByVehicleId(10L)).thenReturn(List.of(reminder));

        BgTollVignetteDTO bgTollVignette = new BgTollVignetteDTO(
                "CA1234BC", LocalDateTime.of(2026, 12, 31, 0, 0), "Активна", true);
        when(bgTollService.lookupVignette("CA1234BC")).thenReturn(Optional.of(bgTollVignette));

        VignetteCheckResponseDTO result = vehicleController.checkVignette(10L, owner);

        assertTrue(result.hasLocalReminder());
        assertTrue(result.bgTollFound());
        assertFalse(result.match());
        assertEquals("Date does not match BGTOLL records", result.message());
    }

    @Test
    void checkVignette_returnsDetected_whenBgTollHasVignetteButNoLocalReminder() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);
        vehicle.setPlateNumber("CA1234BC");

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, owner)).thenReturn(vehicle);
        when(serviceReminderRepository.findByVehicleId(10L)).thenReturn(List.of());

        BgTollVignetteDTO bgTollVignette = new BgTollVignetteDTO(
                "CA1234BC", LocalDateTime.of(2026, 12, 31, 0, 0), "Активна", true);
        when(bgTollService.lookupVignette("CA1234BC")).thenReturn(Optional.of(bgTollVignette));

        VignetteCheckResponseDTO result = vehicleController.checkVignette(10L, owner);

        assertFalse(result.hasLocalReminder());
        assertTrue(result.bgTollFound());
        assertFalse(result.match());
        assertEquals(LocalDate.of(2026, 12, 31), result.bgTollExpiryDate());
        assertEquals("Vignette found via BGTOLL, not yet saved as a reminder", result.message());
    }

    @Test
    void checkVignette_returnsUnavailable_whenBgTollLookupFails() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);
        vehicle.setPlateNumber("CA1234BC");

        ServiceReminder reminder = new ServiceReminder(
                "Vignette renewal", null, null, null, 12, LocalDate.of(2025, 12, 31), vehicle);

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, owner)).thenReturn(vehicle);
        when(serviceReminderRepository.findByVehicleId(10L)).thenReturn(List.of(reminder));
        when(bgTollService.lookupVignette("CA1234BC")).thenReturn(Optional.empty());

        VignetteCheckResponseDTO result = vehicleController.checkVignette(10L, owner);

        assertTrue(result.hasLocalReminder());
        assertFalse(result.bgTollFound());
        assertFalse(result.match());
        assertEquals("BGTOLL lookup unavailable", result.message());
    }

    @Test
    void checkVignette_throwsForbidden_whenNotOwner() {
        User attacker = new User("bob", "bob@test.com", "hashed");
        attacker.setId(2L);

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, attacker))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));

        assertThrows(ResponseStatusException.class,
                () -> vehicleController.checkVignette(10L, attacker));

        verifyNoInteractions(bgTollService);
    }
}