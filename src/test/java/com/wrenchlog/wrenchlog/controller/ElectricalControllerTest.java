package com.wrenchlog.wrenchlog.controller;

import com.wrenchlog.wrenchlog.dto.*;
import com.wrenchlog.wrenchlog.model.*;
import com.wrenchlog.wrenchlog.service.ElectricalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ElectricalControllerTest {

    private ElectricalService electricalService;
    private ElectricalController electricalController;

    private User owner;
    private Vehicle vehicle;
    private ElectricalComponent component;

    @BeforeEach
    void setUp() {
        electricalService = mock(ElectricalService.class);
        electricalController = new ElectricalController(electricalService);

        owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);
        vehicle = new Vehicle("Mercedes", "E220", 2015, 250000, owner);
        vehicle.setId(10L);
        component = new ElectricalComponent("MAF Sensor", "intermittent voltage", vehicle);
        component.setId(100L);
    }

    @Test
    void getComponents_returnsMappedList() {
        when(electricalService.getComponentsForVehicle(10L, owner)).thenReturn(List.of(component));

        List<ElectricalComponentResponseDTO> result = electricalController.getComponents(10L, owner);

        assertEquals(1, result.size());
        assertEquals("MAF Sensor", result.get(0).name());
    }

    @Test
    void createComponent_returnsCreated() {
        ElectricalComponentCreateDTO dto = new ElectricalComponentCreateDTO("MAF Sensor", "intermittent voltage");
        when(electricalService.createComponent(10L, "MAF Sensor", "intermittent voltage", owner)).thenReturn(component);

        ResponseEntity<ElectricalComponentResponseDTO> response = electricalController.createComponent(10L, dto, owner);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("MAF Sensor", response.getBody().name());
    }

    @Test
    void getComponentDetail_returnsPinsAndSessionsMapped() {
        ElectricalPin pin = new ElectricalPin("Ground", "0V", component);
        pin.setId(200L);
        pin.setPosition(1);

        ElectricalSession session = new ElectricalSession("Cold Start", LocalDateTime.now(), "engine cold", component);
        session.setId(300L);

        when(electricalService.getOwnedComponentOrThrow(10L, 100L, owner)).thenReturn(component);
        when(electricalService.getPinsForComponent(100L)).thenReturn(List.of(pin));
        when(electricalService.getSessionsForComponent(100L)).thenReturn(List.of(session));
        when(electricalService.getReadingsForSession(300L)).thenReturn(List.of());

        ElectricalComponentDetailDTO result = electricalController.getComponentDetail(10L, 100L, owner);

        assertEquals("MAF Sensor", result.name());
        assertEquals(1, result.pins().size());
        assertEquals("Ground", result.pins().get(0).name());
        assertEquals(1, result.sessions().size());
        assertEquals("Cold Start", result.sessions().get(0).label());
    }

    @Test
    void updateComponent_returnsUpdatedBody() {
        ElectricalComponentCreateDTO dto = new ElectricalComponentCreateDTO("New Name", "New Desc");
        ElectricalComponent updated = new ElectricalComponent("New Name", "New Desc", vehicle);
        updated.setId(100L);

        when(electricalService.updateComponent(10L, 100L, "New Name", "New Desc", owner)).thenReturn(updated);

        ResponseEntity<ElectricalComponentResponseDTO> response = electricalController.updateComponent(10L, 100L, dto, owner);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("New Name", response.getBody().name());
    }

    @Test
    void deleteComponent_returnsNoContent() {
        ResponseEntity<Void> response = electricalController.deleteComponent(10L, 100L, owner);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(electricalService).deleteComponent(10L, 100L, owner);
    }

    @Test
    void createPin_returnsCreated() {
        ElectricalPinCreateDTO dto = new ElectricalPinCreateDTO("Ground", "0V");
        ElectricalPin pin = new ElectricalPin("Ground", "0V", component);
        pin.setId(200L);
        pin.setPosition(1);

        when(electricalService.createPin(10L, 100L, "Ground", "0V", owner)).thenReturn(pin);

        ResponseEntity<ElectricalPinResponseDTO> response = electricalController.createPin(10L, 100L, dto, owner);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("Ground", response.getBody().name());
        assertEquals(1, response.getBody().position());
    }

    @Test
    void deletePin_propagatesConflict_whenServiceThrows() {
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "has readings"))
                .when(electricalService).deletePin(10L, 100L, 200L, owner);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> electricalController.deletePin(10L, 100L, 200L, owner));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void movePin_returnsNoContent_andDelegatesDirection() {
        ResponseEntity<Void> response = electricalController.movePin(10L, 100L, 200L, "up", owner);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(electricalService).movePin(10L, 100L, 200L, "up", owner);
    }

    @Test
    void createSession_returnsCreatedWithEmptyReadingsList() {
        ElectricalSessionCreateDTO dto = new ElectricalSessionCreateDTO("Cold Start", null, "engine cold");
        ElectricalSession session = new ElectricalSession("Cold Start", LocalDateTime.now(), "engine cold", component);
        session.setId(300L);

        when(electricalService.createSession(10L, 100L, "Cold Start", null, "engine cold", owner)).thenReturn(session);
        when(electricalService.getReadingsForSession(300L)).thenReturn(List.of());

        ResponseEntity<ElectricalSessionResponseDTO> response = electricalController.createSession(10L, 100L, dto, owner);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("Cold Start", response.getBody().label());
        assertTrue(response.getBody().readings().isEmpty());
    }

    @Test
    void upsertReading_returnsOk_whenReadingSaved() {
        ElectricalSession session = new ElectricalSession("Cold Start", LocalDateTime.now(), null, component);
        session.setId(300L);
        ElectricalPin pin = new ElectricalPin("Ground", null, component);
        pin.setId(200L);
        ElectricalReading reading = new ElectricalReading("0.02", "V", session, pin);

        ElectricalReadingUpsertDTO dto = new ElectricalReadingUpsertDTO("0.02", "V");
        when(electricalService.upsertReading(10L, 100L, 300L, 200L, dto, owner)).thenReturn(Optional.of(reading));

        ResponseEntity<ElectricalReadingResponseDTO> response =
                electricalController.upsertReading(10L, 100L, 300L, 200L, dto, owner);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("0.02", response.getBody().value());
    }

    @Test
    void upsertReading_returnsNoContent_whenReadingDeleted() {
        ElectricalReadingUpsertDTO dto = new ElectricalReadingUpsertDTO("", null);
        when(electricalService.upsertReading(10L, 100L, 300L, 200L, dto, owner)).thenReturn(Optional.empty());

        ResponseEntity<ElectricalReadingResponseDTO> response =
                electricalController.upsertReading(10L, 100L, 300L, 200L, dto, owner);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
    }
}