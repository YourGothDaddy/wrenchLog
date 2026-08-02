package com.wrenchlog.wrenchlog.controller;

import com.wrenchlog.wrenchlog.model.User;
import com.wrenchlog.wrenchlog.model.Vehicle;
import com.wrenchlog.wrenchlog.model.VehicleNote;
import com.wrenchlog.wrenchlog.repository.VehicleNoteRepository;
import com.wrenchlog.wrenchlog.service.VehicleAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VehicleNoteControllerTest {

    private VehicleNoteRepository vehicleNoteRepository;
    private VehicleAccessService vehicleAccessService;
    private VehicleNoteController vehicleNoteController;

    @BeforeEach
    void setUp() {
        vehicleNoteRepository = mock(VehicleNoteRepository.class);
        vehicleAccessService = mock(VehicleAccessService.class);
        vehicleNoteController = new VehicleNoteController(vehicleNoteRepository, vehicleAccessService);
    }

    @Test
    void getVehicleNotes_returnsNotes_whenOwner() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);

        VehicleNote note = new VehicleNote("Squeaky brakes", "Front left, check pads", vehicle);
        note.setId(300L);

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, owner)).thenReturn(vehicle);
        when(vehicleNoteRepository.findByVehicleIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(note));

        ResponseEntity<List<VehicleNote>> response = vehicleNoteController.getVehicleNotes(10L, owner);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("Squeaky brakes", response.getBody().get(0).getTitle());

        verify(vehicleAccessService).getOwnedVehicleOrThrow(10L, owner);
    }

    @Test
    void getVehicleNotes_throwsForbidden_whenNotOwner() {
        User attacker = new User("bob", "bob@test.com", "hashed");
        attacker.setId(2L);

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, attacker))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));

        assertThrows(ResponseStatusException.class,
                () -> vehicleNoteController.getVehicleNotes(10L, attacker));

        verify(vehicleNoteRepository, never()).findByVehicleIdOrderByCreatedAtDesc(any());
    }

    @Test
    void createNote_returnsCreated_withCorrectData() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);

        VehicleNote incoming = new VehicleNote("Tire pressure", "Check monthly", null);

        VehicleNote saved = new VehicleNote("Tire pressure", "Check monthly", vehicle);
        saved.setId(301L);

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, owner)).thenReturn(vehicle);
        when(vehicleNoteRepository.save(any(VehicleNote.class))).thenReturn(saved);

        ResponseEntity<?> response = vehicleNoteController.createNote(10L, incoming, owner);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        VehicleNote body = (VehicleNote) response.getBody();
        assertEquals(301L, body.getId());
        assertEquals("Tire pressure", body.getTitle());

        verify(vehicleNoteRepository).save(any(VehicleNote.class));
    }

    @Test
    void createNote_throwsForbidden_whenNotOwner() {
        User attacker = new User("bob", "bob@test.com", "hashed");
        attacker.setId(2L);

        VehicleNote incoming = new VehicleNote("Title", "Content", null);

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, attacker))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));

        assertThrows(ResponseStatusException.class,
                () -> vehicleNoteController.createNote(10L, incoming, attacker));

        verify(vehicleNoteRepository, never()).save(any());
    }

    @Test
    void updateNote_updatesSuccessfully_whenOwner() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);

        VehicleNote existing = new VehicleNote("Old title", "Old content", vehicle);
        existing.setId(300L);

        VehicleNote updatedInput = new VehicleNote("New title", "New content", null);

        when(vehicleNoteRepository.findById(300L)).thenReturn(Optional.of(existing));
        when(vehicleNoteRepository.save(any(VehicleNote.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<?> response = vehicleNoteController.updateNote(10L, 300L, updatedInput, owner);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        VehicleNote body = (VehicleNote) response.getBody();
        assertEquals("New title", body.getTitle());
        assertEquals("New content", body.getContent());

        verify(vehicleAccessService).assertOwnership(vehicle, owner);
    }

    @Test
    void updateNote_throwsNotFound_whenNoteMissing() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        VehicleNote updatedInput = new VehicleNote("Title", "Content", null);

        when(vehicleNoteRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> vehicleNoteController.updateNote(10L, 999L, updatedInput, owner));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(vehicleNoteRepository, never()).save(any());
    }

    @Test
    void updateNote_throwsNotFound_whenNoteBelongsToDifferentVehicle() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);

        VehicleNote existing = new VehicleNote("Title", "Content", vehicle);
        existing.setId(300L);

        VehicleNote updatedInput = new VehicleNote("New title", "New content", null);

        when(vehicleNoteRepository.findById(300L)).thenReturn(Optional.of(existing));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> vehicleNoteController.updateNote(999L, 300L, updatedInput, owner));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(vehicleNoteRepository, never()).save(any());
    }

    @Test
    void updateNote_throwsForbidden_whenNotOwner() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        User attacker = new User("bob", "bob@test.com", "hashed");
        attacker.setId(2L);

        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);

        VehicleNote existing = new VehicleNote("Title", "Content", vehicle);
        existing.setId(300L);

        VehicleNote updatedInput = new VehicleNote("New title", "New content", null);

        when(vehicleNoteRepository.findById(300L)).thenReturn(Optional.of(existing));
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN))
                .when(vehicleAccessService).assertOwnership(vehicle, attacker);

        assertThrows(ResponseStatusException.class,
                () -> vehicleNoteController.updateNote(10L, 300L, updatedInput, attacker));

        verify(vehicleNoteRepository, never()).save(any());
    }

    @Test
    void deleteNote_deletesSuccessfully_whenOwner() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);

        VehicleNote note = new VehicleNote("Title", "Content", vehicle);
        note.setId(300L);

        when(vehicleNoteRepository.findById(300L)).thenReturn(Optional.of(note));

        ResponseEntity<Void> response = vehicleNoteController.deleteNote(10L, 300L, owner);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(vehicleNoteRepository).deleteById(300L);
    }

    @Test
    void deleteNote_throwsNotFound_whenNoteMissing() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        when(vehicleNoteRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> vehicleNoteController.deleteNote(10L, 999L, owner));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(vehicleNoteRepository, never()).deleteById(any());
    }

    @Test
    void deleteNote_throwsNotFound_whenNoteBelongsToDifferentVehicle() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);

        VehicleNote note = new VehicleNote("Title", "Content", vehicle);
        note.setId(300L);

        when(vehicleNoteRepository.findById(300L)).thenReturn(Optional.of(note));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> vehicleNoteController.deleteNote(999L, 300L, owner));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(vehicleNoteRepository, never()).deleteById(any());
    }

    @Test
    void deleteNote_throwsForbidden_whenNotOwner() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        User attacker = new User("bob", "bob@test.com", "hashed");
        attacker.setId(2L);

        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);

        VehicleNote note = new VehicleNote("Title", "Content", vehicle);
        note.setId(300L);

        when(vehicleNoteRepository.findById(300L)).thenReturn(Optional.of(note));
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN))
                .when(vehicleAccessService).assertOwnership(vehicle, attacker);

        assertThrows(ResponseStatusException.class,
                () -> vehicleNoteController.deleteNote(10L, 300L, attacker));

        verify(vehicleNoteRepository, never()).deleteById(any());
    }
}