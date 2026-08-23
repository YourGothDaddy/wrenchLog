package com.wrenchlog.wrenchlog.controller;

import com.wrenchlog.wrenchlog.dto.VehicleFolderCreateDTO;
import com.wrenchlog.wrenchlog.dto.VehicleFolderResponseDTO;
import com.wrenchlog.wrenchlog.model.User;
import com.wrenchlog.wrenchlog.model.Vehicle;
import com.wrenchlog.wrenchlog.model.VehicleFile;
import com.wrenchlog.wrenchlog.model.VehicleFolder;
import com.wrenchlog.wrenchlog.repository.VehicleFileRepository;
import com.wrenchlog.wrenchlog.repository.VehicleFolderRepository;
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

class VehicleFolderControllerTest {

    private VehicleFolderRepository vehicleFolderRepository;
    private VehicleFileRepository vehicleFileRepository;
    private VehicleAccessService vehicleAccessService;
    private VehicleFolderController vehicleFolderController;

    @BeforeEach
    void setUp() {
        vehicleFolderRepository = mock(VehicleFolderRepository.class);
        vehicleFileRepository = mock(VehicleFileRepository.class);
        vehicleAccessService = mock(VehicleAccessService.class);
        vehicleFolderController = new VehicleFolderController(vehicleFolderRepository, vehicleFileRepository, vehicleAccessService);
    }

    @Test
    void createFolder_returnsCreated_withCorrectData() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);
        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);

        VehicleFolder saved = new VehicleFolder("Manuals", vehicle);
        saved.setId(50L);

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, owner)).thenReturn(vehicle);
        when(vehicleFolderRepository.save(any(VehicleFolder.class))).thenReturn(saved);
        when(vehicleFileRepository.findByVehicleIdAndFolderId(10L, 50L)).thenReturn(List.of());

        ResponseEntity<VehicleFolderResponseDTO> response =
                vehicleFolderController.createFolder(10L, new VehicleFolderCreateDTO("Manuals"), owner);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(50L, response.getBody().id());
        assertEquals("Manuals", response.getBody().name());
        assertEquals(10L, response.getBody().vehicleId());
        assertEquals(0L, response.getBody().fileCount());
    }

    @Test
    void createFolder_throwsForbidden_whenNotOwner() {
        User attacker = new User("bob", "bob@test.com", "hashed");
        attacker.setId(2L);

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, attacker))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));

        assertThrows(ResponseStatusException.class,
                () -> vehicleFolderController.createFolder(10L, new VehicleFolderCreateDTO("Manuals"), attacker));

        verify(vehicleFolderRepository, never()).save(any());
    }

    @Test
    void getVehicleFolders_returnsFoldersWithFileCounts() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);
        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);

        VehicleFolder folder = new VehicleFolder("Manuals", vehicle);
        folder.setId(50L);

        VehicleFile fileInFolder = new VehicleFile("manual.pdf", "application/pdf", "/uploads/manual.pdf", vehicle, folder);
        fileInFolder.setId(400L);

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, owner)).thenReturn(vehicle);
        when(vehicleFolderRepository.findByVehicleId(10L)).thenReturn(List.of(folder));
        when(vehicleFileRepository.findByVehicleIdAndFolderId(10L, 50L)).thenReturn(List.of(fileInFolder));

        ResponseEntity<List<VehicleFolderResponseDTO>> response =
                vehicleFolderController.getVehicleFolders(10L, owner);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals(1L, response.getBody().get(0).fileCount());
    }

    @Test
    void getVehicleFolders_throwsForbidden_whenNotOwner() {
        User attacker = new User("bob", "bob@test.com", "hashed");
        attacker.setId(2L);

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, attacker))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));

        assertThrows(ResponseStatusException.class,
                () -> vehicleFolderController.getVehicleFolders(10L, attacker));

        verify(vehicleFolderRepository, never()).findByVehicleId(any());
    }

    @Test
    void renameFolder_returnsOk_withUpdatedName() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);
        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);

        VehicleFolder folder = new VehicleFolder("Manuals", vehicle);
        folder.setId(50L);

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, owner)).thenReturn(vehicle);
        when(vehicleFolderRepository.findById(50L)).thenReturn(Optional.of(folder));
        when(vehicleFolderRepository.save(any(VehicleFolder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(vehicleFileRepository.findByVehicleIdAndFolderId(10L, 50L)).thenReturn(List.of());

        ResponseEntity<VehicleFolderResponseDTO> response =
                vehicleFolderController.renameFolder(10L, 50L, new VehicleFolderCreateDTO("Owner's Manuals"), owner);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Owner's Manuals", response.getBody().name());
    }

    @Test
    void renameFolder_throwsNotFound_whenFolderMissing() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);
        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, owner)).thenReturn(vehicle);
        when(vehicleFolderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                () -> vehicleFolderController.renameFolder(10L, 999L, new VehicleFolderCreateDTO("New Name"), owner));

        verify(vehicleFolderRepository, never()).save(any());
    }

    @Test
    void renameFolder_throwsForbidden_whenFolderBelongsToDifferentVehicle() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);
        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);

        Vehicle otherVehicle = new Vehicle("Honda", "Civic", 2018, 30000, owner);
        otherVehicle.setId(20L);

        VehicleFolder folder = new VehicleFolder("Manuals", otherVehicle);
        folder.setId(50L);

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, owner)).thenReturn(vehicle);
        when(vehicleFolderRepository.findById(50L)).thenReturn(Optional.of(folder));

        assertThrows(ResponseStatusException.class,
                () -> vehicleFolderController.renameFolder(10L, 50L, new VehicleFolderCreateDTO("New Name"), owner));

        verify(vehicleFolderRepository, never()).save(any());
    }

    @Test
    void deleteFolder_returnsNoContent_whenOwner() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);
        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);

        VehicleFolder folder = new VehicleFolder("Manuals", vehicle);
        folder.setId(50L);

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, owner)).thenReturn(vehicle);
        when(vehicleFolderRepository.findById(50L)).thenReturn(Optional.of(folder));

        ResponseEntity<Void> response = vehicleFolderController.deleteFolder(10L, 50L, owner);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(vehicleFolderRepository).delete(folder);
    }

    @Test
    void deleteFolder_throwsNotFound_whenFolderMissing() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);
        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, owner)).thenReturn(vehicle);
        when(vehicleFolderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                () -> vehicleFolderController.deleteFolder(10L, 999L, owner));

        verify(vehicleFolderRepository, never()).delete(any());
    }

    @Test
    void deleteFolder_throwsForbidden_whenFolderBelongsToDifferentVehicle() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);
        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);

        Vehicle otherVehicle = new Vehicle("Honda", "Civic", 2018, 30000, owner);
        otherVehicle.setId(20L);

        VehicleFolder folder = new VehicleFolder("Manuals", otherVehicle);
        folder.setId(50L);

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, owner)).thenReturn(vehicle);
        when(vehicleFolderRepository.findById(50L)).thenReturn(Optional.of(folder));

        assertThrows(ResponseStatusException.class,
                () -> vehicleFolderController.deleteFolder(10L, 50L, owner));

        verify(vehicleFolderRepository, never()).delete(any());
    }

    @Test
    void deleteFolder_throwsForbidden_whenNotVehicleOwner() {
        User attacker = new User("bob", "bob@test.com", "hashed");
        attacker.setId(2L);

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, attacker))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));

        assertThrows(ResponseStatusException.class,
                () -> vehicleFolderController.deleteFolder(10L, 50L, attacker));

        verify(vehicleFolderRepository, never()).findById(any());
    }
}