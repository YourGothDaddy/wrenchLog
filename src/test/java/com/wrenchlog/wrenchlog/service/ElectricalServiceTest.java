package com.wrenchlog.wrenchlog.service;

import com.wrenchlog.wrenchlog.dto.ElectricalReadingUpsertDTO;
import com.wrenchlog.wrenchlog.model.*;
import com.wrenchlog.wrenchlog.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ElectricalServiceTest {

    private VehicleAccessService vehicleAccessService;
    private ElectricalComponentRepository componentRepository;
    private ElectricalPinRepository pinRepository;
    private ElectricalSessionRepository sessionRepository;
    private ElectricalReadingRepository readingRepository;
    private ElectricalService electricalService;

    private User owner;
    private User attacker;
    private Vehicle vehicle;

    @BeforeEach
    void setUp() {
        vehicleAccessService = mock(VehicleAccessService.class);
        componentRepository = mock(ElectricalComponentRepository.class);
        pinRepository = mock(ElectricalPinRepository.class);
        sessionRepository = mock(ElectricalSessionRepository.class);
        readingRepository = mock(ElectricalReadingRepository.class);

        electricalService = new ElectricalService(vehicleAccessService, componentRepository,
                pinRepository, sessionRepository, readingRepository);

        owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);
        attacker = new User("bob", "bob@test.com", "hashed");
        attacker.setId(2L);
        vehicle = new Vehicle("Mercedes", "E220", 2015, 250000, owner);
        vehicle.setId(10L);
    }

    @Test
    void getOwnedComponentOrThrow_returnsComponent_whenOwnerAndCorrectVehicle() {
        ElectricalComponent component = new ElectricalComponent("MAF Sensor", null, vehicle);
        component.setId(100L);

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, owner)).thenReturn(vehicle);
        when(componentRepository.findById(100L)).thenReturn(Optional.of(component));

        ElectricalComponent result = electricalService.getOwnedComponentOrThrow(10L, 100L, owner);

        assertEquals(component, result);
    }

    @Test
    void getOwnedComponentOrThrow_throwsNotFound_whenComponentMissing() {
        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, owner)).thenReturn(vehicle);
        when(componentRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> electricalService.getOwnedComponentOrThrow(10L, 999L, owner));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void getOwnedComponentOrThrow_throwsNotFound_whenComponentBelongsToDifferentVehicle() {
        Vehicle otherVehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        otherVehicle.setId(20L);
        ElectricalComponent component = new ElectricalComponent("MAF Sensor", null, otherVehicle);
        component.setId(100L);

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, owner)).thenReturn(vehicle);
        when(componentRepository.findById(100L)).thenReturn(Optional.of(component));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> electricalService.getOwnedComponentOrThrow(10L, 100L, owner));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void getOwnedComponentOrThrow_throwsForbidden_whenNotOwner() {
        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, attacker))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));

        assertThrows(ResponseStatusException.class,
                () -> electricalService.getOwnedComponentOrThrow(10L, 100L, attacker));

        verifyNoInteractions(componentRepository);
    }

    @Test
    void getOwnedPinOrThrow_returnsPin_whenBelongsToComponent() {
        ElectricalComponent component = new ElectricalComponent("MAF Sensor", null, vehicle);
        component.setId(100L);
        ElectricalPin pin = new ElectricalPin("Ground", null, component);
        pin.setId(200L);

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, owner)).thenReturn(vehicle);
        when(componentRepository.findById(100L)).thenReturn(Optional.of(component));
        when(pinRepository.findById(200L)).thenReturn(Optional.of(pin));

        ElectricalPin result = electricalService.getOwnedPinOrThrow(10L, 100L, 200L, owner);

        assertEquals(pin, result);
    }

    @Test
    void getOwnedPinOrThrow_throwsNotFound_whenPinBelongsToDifferentComponent() {
        ElectricalComponent component = new ElectricalComponent("MAF Sensor", null, vehicle);
        component.setId(100L);
        ElectricalComponent otherComponent = new ElectricalComponent("O2 Sensor", null, vehicle);
        otherComponent.setId(101L);
        ElectricalPin pin = new ElectricalPin("Ground", null, otherComponent);
        pin.setId(200L);

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, owner)).thenReturn(vehicle);
        when(componentRepository.findById(100L)).thenReturn(Optional.of(component));
        when(pinRepository.findById(200L)).thenReturn(Optional.of(pin));

        assertThrows(ResponseStatusException.class,
                () -> electricalService.getOwnedPinOrThrow(10L, 100L, 200L, owner));
    }

    @Test
    void getOwnedSessionOrThrow_throwsNotFound_whenSessionBelongsToDifferentComponent() {
        ElectricalComponent component = new ElectricalComponent("MAF Sensor", null, vehicle);
        component.setId(100L);
        ElectricalComponent otherComponent = new ElectricalComponent("O2 Sensor", null, vehicle);
        otherComponent.setId(101L);
        ElectricalSession session = new ElectricalSession("Cold Start", LocalDateTime.now(), null, otherComponent);
        session.setId(300L);

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, owner)).thenReturn(vehicle);
        when(componentRepository.findById(100L)).thenReturn(Optional.of(component));
        when(sessionRepository.findById(300L)).thenReturn(Optional.of(session));

        assertThrows(ResponseStatusException.class,
                () -> electricalService.getOwnedSessionOrThrow(10L, 100L, 300L, owner));
    }

    @Test
    void createPin_assignsPositionOne_whenFirstPin() {
        ElectricalComponent component = new ElectricalComponent("MAF Sensor", null, vehicle);
        component.setId(100L);

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, owner)).thenReturn(vehicle);
        when(componentRepository.findById(100L)).thenReturn(Optional.of(component));
        when(pinRepository.findByComponentIdOrderByPositionAsc(100L)).thenReturn(List.of());
        when(pinRepository.save(any(ElectricalPin.class))).thenAnswer(inv -> inv.getArgument(0));

        ElectricalPin result = electricalService.createPin(10L, 100L, "Ground", null, owner);

        assertEquals(1, result.getPosition());
    }

    @Test
    void createPin_assignsNextPosition_whenPinsAlreadyExist() {
        ElectricalComponent component = new ElectricalComponent("MAF Sensor", null, vehicle);
        component.setId(100L);

        ElectricalPin existing1 = new ElectricalPin("Ground", null, component);
        existing1.setPosition(1);
        ElectricalPin existing2 = new ElectricalPin("Signal", null, component);
        existing2.setPosition(2);

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, owner)).thenReturn(vehicle);
        when(componentRepository.findById(100L)).thenReturn(Optional.of(component));
        when(pinRepository.findByComponentIdOrderByPositionAsc(100L)).thenReturn(List.of(existing1, existing2));
        when(pinRepository.save(any(ElectricalPin.class))).thenAnswer(inv -> inv.getArgument(0));

        ElectricalPin result = electricalService.createPin(10L, 100L, "5V Ref", null, owner);

        assertEquals(3, result.getPosition());
    }

    @Test
    void deletePin_succeeds_whenNoReadingsExist() {
        ElectricalComponent component = new ElectricalComponent("MAF Sensor", null, vehicle);
        component.setId(100L);
        ElectricalPin pin = new ElectricalPin("Ground", null, component);
        pin.setId(200L);

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, owner)).thenReturn(vehicle);
        when(componentRepository.findById(100L)).thenReturn(Optional.of(component));
        when(pinRepository.findById(200L)).thenReturn(Optional.of(pin));
        when(readingRepository.findByPinId(200L)).thenReturn(List.of());

        assertDoesNotThrow(() -> electricalService.deletePin(10L, 100L, 200L, owner));

        verify(pinRepository).delete(pin);
    }

    @Test
    void deletePin_throwsConflict_whenReadingsExist() {
        ElectricalComponent component = new ElectricalComponent("MAF Sensor", null, vehicle);
        component.setId(100L);
        ElectricalPin pin = new ElectricalPin("Ground", null, component);
        pin.setId(200L);
        ElectricalSession session = new ElectricalSession("Cold Start", LocalDateTime.now(), null, component);
        ElectricalReading reading = new ElectricalReading("0.02", "V", session, pin);

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, owner)).thenReturn(vehicle);
        when(componentRepository.findById(100L)).thenReturn(Optional.of(component));
        when(pinRepository.findById(200L)).thenReturn(Optional.of(pin));
        when(readingRepository.findByPinId(200L)).thenReturn(List.of(reading));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> electricalService.deletePin(10L, 100L, 200L, owner));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(pinRepository, never()).delete(any());
    }

    @Test
    void movePin_swapsPositions_whenMovingUp() {
        ElectricalComponent component = new ElectricalComponent("MAF Sensor", null, vehicle);
        component.setId(100L);

        ElectricalPin pin1 = new ElectricalPin("Ground", null, component);
        pin1.setId(200L);
        pin1.setPosition(1);
        ElectricalPin pin2 = new ElectricalPin("Signal", null, component);
        pin2.setId(201L);
        pin2.setPosition(2);

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, owner)).thenReturn(vehicle);
        when(componentRepository.findById(100L)).thenReturn(Optional.of(component));
        when(pinRepository.findByComponentIdOrderByPositionAsc(100L)).thenReturn(List.of(pin1, pin2));

        electricalService.movePin(10L, 100L, 201L, "up", owner);

        assertEquals(2, pin1.getPosition());
        assertEquals(1, pin2.getPosition());
        verify(pinRepository).save(pin1);
        verify(pinRepository).save(pin2);
    }

    @Test
    void movePin_doesNothing_whenAlreadyAtTop() {
        ElectricalComponent component = new ElectricalComponent("MAF Sensor", null, vehicle);
        component.setId(100L);

        ElectricalPin pin1 = new ElectricalPin("Ground", null, component);
        pin1.setId(200L);
        pin1.setPosition(1);
        ElectricalPin pin2 = new ElectricalPin("Signal", null, component);
        pin2.setId(201L);
        pin2.setPosition(2);

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, owner)).thenReturn(vehicle);
        when(componentRepository.findById(100L)).thenReturn(Optional.of(component));
        when(pinRepository.findByComponentIdOrderByPositionAsc(100L)).thenReturn(List.of(pin1, pin2));

        electricalService.movePin(10L, 100L, 200L, "up", owner);

        assertEquals(1, pin1.getPosition());
        assertEquals(2, pin2.getPosition());
        verify(pinRepository, never()).save(any());
    }

    @Test
    void movePin_doesNothing_whenAlreadyAtBottom() {
        ElectricalComponent component = new ElectricalComponent("MAF Sensor", null, vehicle);
        component.setId(100L);

        ElectricalPin pin1 = new ElectricalPin("Ground", null, component);
        pin1.setId(200L);
        pin1.setPosition(1);
        ElectricalPin pin2 = new ElectricalPin("Signal", null, component);
        pin2.setId(201L);
        pin2.setPosition(2);

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, owner)).thenReturn(vehicle);
        when(componentRepository.findById(100L)).thenReturn(Optional.of(component));
        when(pinRepository.findByComponentIdOrderByPositionAsc(100L)).thenReturn(List.of(pin1, pin2));

        electricalService.movePin(10L, 100L, 201L, "down", owner);

        assertEquals(1, pin1.getPosition());
        assertEquals(2, pin2.getPosition());
        verify(pinRepository, never()).save(any());
    }

    @Test
    void movePin_throwsNotFound_whenPinNotInComponent() {
        ElectricalComponent component = new ElectricalComponent("MAF Sensor", null, vehicle);
        component.setId(100L);

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, owner)).thenReturn(vehicle);
        when(componentRepository.findById(100L)).thenReturn(Optional.of(component));
        when(pinRepository.findByComponentIdOrderByPositionAsc(100L)).thenReturn(List.of());

        assertThrows(ResponseStatusException.class,
                () -> electricalService.movePin(10L, 100L, 999L, "up", owner));
    }

    @Test
    void createSession_usesProvidedDate_whenGiven() {
        ElectricalComponent component = new ElectricalComponent("MAF Sensor", null, vehicle);
        component.setId(100L);
        LocalDateTime providedDate = LocalDateTime.of(2026, 1, 1, 9, 0);

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, owner)).thenReturn(vehicle);
        when(componentRepository.findById(100L)).thenReturn(Optional.of(component));
        when(sessionRepository.save(any(ElectricalSession.class))).thenAnswer(inv -> inv.getArgument(0));

        ElectricalSession result = electricalService.createSession(10L, 100L, "Cold Start", providedDate, "engine cold", owner);

        assertEquals(providedDate, result.getSessionDate());
    }

    @Test
    void createSession_defaultsToNow_whenDateNotProvided() {
        ElectricalComponent component = new ElectricalComponent("MAF Sensor", null, vehicle);
        component.setId(100L);

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, owner)).thenReturn(vehicle);
        when(componentRepository.findById(100L)).thenReturn(Optional.of(component));
        when(sessionRepository.save(any(ElectricalSession.class))).thenAnswer(inv -> inv.getArgument(0));

        ElectricalSession result = electricalService.createSession(10L, 100L, "Cold Start", null, null, owner);

        assertNotNull(result.getSessionDate());
    }

    @Test
    void upsertReading_createsNewReading_whenNoneExists() {
        ElectricalComponent component = new ElectricalComponent("MAF Sensor", null, vehicle);
        component.setId(100L);
        ElectricalSession session = new ElectricalSession("Cold Start", LocalDateTime.now(), null, component);
        session.setId(300L);
        ElectricalPin pin = new ElectricalPin("Ground", null, component);
        pin.setId(200L);

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, owner)).thenReturn(vehicle);
        when(componentRepository.findById(100L)).thenReturn(Optional.of(component));
        when(sessionRepository.findById(300L)).thenReturn(Optional.of(session));
        when(pinRepository.findById(200L)).thenReturn(Optional.of(pin));
        when(readingRepository.findBySessionIdAndPinId(300L, 200L)).thenReturn(Optional.empty());
        when(readingRepository.save(any(ElectricalReading.class))).thenAnswer(inv -> inv.getArgument(0));

        ElectricalReadingUpsertDTO dto = new ElectricalReadingUpsertDTO("0.02", "V");
        Optional<ElectricalReading> result = electricalService.upsertReading(10L, 100L, 300L, 200L, dto, owner);

        assertTrue(result.isPresent());
        assertEquals("0.02", result.get().getValue());
        assertEquals("V", result.get().getUnit());
    }

    @Test
    void upsertReading_updatesExistingReading_whenAlreadyExists() {
        ElectricalComponent component = new ElectricalComponent("MAF Sensor", null, vehicle);
        component.setId(100L);
        ElectricalSession session = new ElectricalSession("Cold Start", LocalDateTime.now(), null, component);
        session.setId(300L);
        ElectricalPin pin = new ElectricalPin("Ground", null, component);
        pin.setId(200L);
        ElectricalReading existingReading = new ElectricalReading("0.01", "V", session, pin);
        existingReading.setId(400L);

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, owner)).thenReturn(vehicle);
        when(componentRepository.findById(100L)).thenReturn(Optional.of(component));
        when(sessionRepository.findById(300L)).thenReturn(Optional.of(session));
        when(pinRepository.findById(200L)).thenReturn(Optional.of(pin));
        when(readingRepository.findBySessionIdAndPinId(300L, 200L)).thenReturn(Optional.of(existingReading));
        when(readingRepository.save(any(ElectricalReading.class))).thenAnswer(inv -> inv.getArgument(0));

        ElectricalReadingUpsertDTO dto = new ElectricalReadingUpsertDTO("0.09", "V");
        Optional<ElectricalReading> result = electricalService.upsertReading(10L, 100L, 300L, 200L, dto, owner);

        assertTrue(result.isPresent());
        assertEquals(400L, result.get().getId());
        assertEquals("0.09", result.get().getValue());
    }

    @Test
    void upsertReading_deletesReading_whenValueBlankedAndReadingExists() {
        ElectricalComponent component = new ElectricalComponent("MAF Sensor", null, vehicle);
        component.setId(100L);
        ElectricalSession session = new ElectricalSession("Cold Start", LocalDateTime.now(), null, component);
        session.setId(300L);
        ElectricalPin pin = new ElectricalPin("Ground", null, component);
        pin.setId(200L);
        ElectricalReading existingReading = new ElectricalReading("0.01", "V", session, pin);
        existingReading.setId(400L);

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, owner)).thenReturn(vehicle);
        when(componentRepository.findById(100L)).thenReturn(Optional.of(component));
        when(sessionRepository.findById(300L)).thenReturn(Optional.of(session));
        when(pinRepository.findById(200L)).thenReturn(Optional.of(pin));
        when(readingRepository.findBySessionIdAndPinId(300L, 200L)).thenReturn(Optional.of(existingReading));

        ElectricalReadingUpsertDTO dto = new ElectricalReadingUpsertDTO("", null);
        Optional<ElectricalReading> result = electricalService.upsertReading(10L, 100L, 300L, 200L, dto, owner);

        assertTrue(result.isEmpty());
        verify(readingRepository).delete(existingReading);
        verify(readingRepository, never()).save(any());
    }

    @Test
    void upsertReading_doesNothing_whenValueBlankAndNoReadingExists() {
        ElectricalComponent component = new ElectricalComponent("MAF Sensor", null, vehicle);
        component.setId(100L);
        ElectricalSession session = new ElectricalSession("Cold Start", LocalDateTime.now(), null, component);
        session.setId(300L);
        ElectricalPin pin = new ElectricalPin("Ground", null, component);
        pin.setId(200L);

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, owner)).thenReturn(vehicle);
        when(componentRepository.findById(100L)).thenReturn(Optional.of(component));
        when(sessionRepository.findById(300L)).thenReturn(Optional.of(session));
        when(pinRepository.findById(200L)).thenReturn(Optional.of(pin));
        when(readingRepository.findBySessionIdAndPinId(300L, 200L)).thenReturn(Optional.empty());

        ElectricalReadingUpsertDTO dto = new ElectricalReadingUpsertDTO("   ", null);
        Optional<ElectricalReading> result = electricalService.upsertReading(10L, 100L, 300L, 200L, dto, owner);

        assertTrue(result.isEmpty());
        verify(readingRepository, never()).delete(any());
        verify(readingRepository, never()).save(any());
    }

    @Test
    void createComponent_savesWithVehicleAttached() {
        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, owner)).thenReturn(vehicle);
        when(componentRepository.save(any(ElectricalComponent.class))).thenAnswer(inv -> inv.getArgument(0));

        ElectricalComponent result = electricalService.createComponent(10L, "MAF Sensor", "intermittent voltage", owner);

        assertEquals("MAF Sensor", result.getName());
        assertEquals(vehicle, result.getVehicle());
    }

    @Test
    void updateComponent_updatesNameAndDescription() {
        ElectricalComponent component = new ElectricalComponent("Old Name", "Old Desc", vehicle);
        component.setId(100L);

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, owner)).thenReturn(vehicle);
        when(componentRepository.findById(100L)).thenReturn(Optional.of(component));
        when(componentRepository.save(any(ElectricalComponent.class))).thenAnswer(inv -> inv.getArgument(0));

        ElectricalComponent result = electricalService.updateComponent(10L, 100L, "New Name", "New Desc", owner);

        assertEquals("New Name", result.getName());
        assertEquals("New Desc", result.getDescription());
    }

    @Test
    void deleteComponent_throwsForbidden_whenNotOwner() {
        ElectricalComponent component = new ElectricalComponent("MAF Sensor", null, vehicle);
        component.setId(100L);

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, attacker))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));

        assertThrows(ResponseStatusException.class,
                () -> electricalService.deleteComponent(10L, 100L, attacker));

        verify(componentRepository, never()).delete(any());
    }
}