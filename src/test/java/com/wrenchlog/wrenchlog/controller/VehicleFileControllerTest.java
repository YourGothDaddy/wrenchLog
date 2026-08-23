package com.wrenchlog.wrenchlog.controller;

import com.wrenchlog.wrenchlog.dto.FileDownloadDto;
import com.wrenchlog.wrenchlog.dto.VehicleFileResponseDTO;
import com.wrenchlog.wrenchlog.model.User;
import com.wrenchlog.wrenchlog.model.Vehicle;
import com.wrenchlog.wrenchlog.model.VehicleFile;
import com.wrenchlog.wrenchlog.model.VehicleFolder;
import com.wrenchlog.wrenchlog.repository.VehicleFileRepository;
import com.wrenchlog.wrenchlog.security.JwtService;
import com.wrenchlog.wrenchlog.service.FileStorageService;
import com.wrenchlog.wrenchlog.service.VehicleAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VehicleFileControllerTest {

    private FileStorageService fileStorageService;
    private VehicleFileRepository vehicleFileRepository;
    private VehicleAccessService vehicleAccessService;
    private JwtService jwtService;
    private VehicleFileController vehicleFileController;

    @BeforeEach
    void setUp() {
        fileStorageService = mock(FileStorageService.class);
        vehicleFileRepository = mock(VehicleFileRepository.class);
        vehicleAccessService = mock(VehicleAccessService.class);
        jwtService = mock(JwtService.class);
        vehicleFileController = new VehicleFileController(fileStorageService, vehicleFileRepository, vehicleAccessService, jwtService);
    }

    @Test
    void uploadFile_returnsOk_withCorrectData_whenNoFolder() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);
        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);

        MockMultipartFile file = new MockMultipartFile("file", "manual.pdf", "application/pdf", "content".getBytes());

        VehicleFile savedFile = new VehicleFile("manual.pdf", "application/pdf", "/uploads/uuid_manual.pdf", vehicle);
        savedFile.setId(400L);

        when(fileStorageService.storeFile(file, 10L, null, owner)).thenReturn(savedFile);

        ResponseEntity<VehicleFileResponseDTO> response = vehicleFileController.uploadFile(10L, file, null, owner);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(400L, response.getBody().id());
        assertEquals("manual.pdf", response.getBody().fileName());
        assertNull(response.getBody().folderId());
    }

    @Test
    void uploadFile_returnsOk_withFolderId_whenUploadedIntoFolder() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);
        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);

        VehicleFolder folder = new VehicleFolder("Manuals", vehicle);
        folder.setId(50L);

        MockMultipartFile file = new MockMultipartFile("file", "manual.pdf", "application/pdf", "content".getBytes());

        VehicleFile savedFile = new VehicleFile("manual.pdf", "application/pdf", "/uploads/uuid_manual.pdf", vehicle, folder);
        savedFile.setId(400L);

        when(fileStorageService.storeFile(file, 10L, 50L, owner)).thenReturn(savedFile);

        ResponseEntity<VehicleFileResponseDTO> response = vehicleFileController.uploadFile(10L, file, 50L, owner);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(50L, response.getBody().folderId());
    }

    @Test
    void getVehicleFiles_returnsRootFiles_whenFolderIdOmitted() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);
        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);

        VehicleFile file = new VehicleFile("manual.pdf", "application/pdf", "/uploads/uuid_manual.pdf", vehicle);
        file.setId(400L);

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, owner)).thenReturn(vehicle);
        when(vehicleFileRepository.findByVehicleIdAndFolderIsNull(10L)).thenReturn(List.of(file));

        ResponseEntity<List<VehicleFileResponseDTO>> response = vehicleFileController.getVehicleFiles(10L, null, owner);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertNull(response.getBody().get(0).folderId());
        verify(vehicleFileRepository, never()).findByVehicleIdAndFolderId(any(), any());
    }

    @Test
    void getVehicleFiles_returnsFolderFiles_whenFolderIdProvided() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);
        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);

        VehicleFolder folder = new VehicleFolder("Manuals", vehicle);
        folder.setId(50L);

        VehicleFile file = new VehicleFile("manual.pdf", "application/pdf", "/uploads/uuid_manual.pdf", vehicle, folder);
        file.setId(400L);

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, owner)).thenReturn(vehicle);
        when(vehicleFileRepository.findByVehicleIdAndFolderId(10L, 50L)).thenReturn(List.of(file));

        ResponseEntity<List<VehicleFileResponseDTO>> response = vehicleFileController.getVehicleFiles(10L, 50L, owner);

        assertEquals(1, response.getBody().size());
        assertEquals(50L, response.getBody().get(0).folderId());
        verify(vehicleFileRepository, never()).findByVehicleIdAndFolderIsNull(any());
    }

    @Test
    void getVehicleFiles_throwsForbidden_whenNotOwner() {
        User attacker = new User("bob", "bob@test.com", "hashed");
        attacker.setId(2L);

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, attacker))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));

        assertThrows(ResponseStatusException.class,
                () -> vehicleFileController.getVehicleFiles(10L, null, attacker));

        verify(vehicleFileRepository, never()).findByVehicleIdAndFolderIsNull(any());
    }

    @Test
    void downloadFile_returnsFile_whenTokenValid() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);

        Resource resource = new ByteArrayResource("file bytes".getBytes());
        FileDownloadDto downloadDto = new FileDownloadDto(resource, "application/pdf", "manual.pdf");

        when(fileStorageService.loadFileByIdOnly(400L)).thenReturn(downloadDto);

        ResponseEntity<Resource> response = vehicleFileController.downloadFile(10L, 400L, "valid-token");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("application/pdf", response.getHeaders().getContentType().toString());
        assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).contains("manual.pdf"));

        verify(jwtService).validateAndExtractDownloadToken("valid-token", 400L);
    }

    @Test
    void downloadFile_returnsForbidden_whenTokenInvalid() {
        doThrow(new SecurityException("Invalid download token"))
                .when(jwtService).validateAndExtractDownloadToken("bad-token", 400L);

        ResponseEntity<Resource> response = vehicleFileController.downloadFile(10L, 400L, "bad-token");

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(fileStorageService, never()).loadFileByIdOnly(any());
    }

    @Test
    void deleteFile_returnsNoContent_whenOwner() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        ResponseEntity<Void> response = vehicleFileController.deleteFile(10L, 400L, owner);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(fileStorageService).deleteFile(400L, 10L, owner);
    }

    @Test
    void deleteFile_throwsSecurityException_whenNotOwner() {
        User attacker = new User("bob", "bob@test.com", "hashed");
        attacker.setId(2L);

        doThrow(new SecurityException("Access Denied"))
                .when(fileStorageService).deleteFile(400L, 10L, attacker);

        assertThrows(SecurityException.class,
                () -> vehicleFileController.deleteFile(10L, 400L, attacker));
    }

    @Test
    void deleteFile_throwsIllegalArgumentException_whenFileMismatched() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        doThrow(new IllegalArgumentException("File does not belong to vehicle"))
                .when(fileStorageService).deleteFile(999L, 10L, owner);

        assertThrows(IllegalArgumentException.class,
                () -> vehicleFileController.deleteFile(10L, 999L, owner));
    }

    @Test
    void deleteFile_throwsRuntimeException_whenUnexpectedErrorOccurs() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        doThrow(new RuntimeException("disk error"))
                .when(fileStorageService).deleteFile(400L, 10L, owner);

        assertThrows(RuntimeException.class,
                () -> vehicleFileController.deleteFile(10L, 400L, owner));
    }

    @Test
    void getDownloadToken_returnsToken_whenOwner() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, owner)).thenReturn(vehicle);
        when(jwtService.generateFileDownloadToken(400L, 1L)).thenReturn("generated-token");

        ResponseEntity<Map<String, String>> response = vehicleFileController.getDownloadToken(10L, 400L, owner);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("generated-token", response.getBody().get("token"));
    }

    @Test
    void getDownloadToken_throwsForbidden_whenNotOwner() {
        User attacker = new User("bob", "bob@test.com", "hashed");
        attacker.setId(2L);

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, attacker))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));

        assertThrows(ResponseStatusException.class,
                () -> vehicleFileController.getDownloadToken(10L, 400L, attacker));

        verify(jwtService, never()).generateFileDownloadToken(any(), any());
    }

    @Test
    void moveFile_returnsOk_withUpdatedFolderId() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);
        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);

        VehicleFolder folder = new VehicleFolder("Manuals", vehicle);
        folder.setId(50L);

        VehicleFile movedFile = new VehicleFile("manual.pdf", "application/pdf", "/uploads/uuid_manual.pdf", vehicle, folder);
        movedFile.setId(400L);

        Map<String, Long> body = new HashMap<>();
        body.put("folderId", 50L);

        when(fileStorageService.moveFile(400L, 10L, 50L, owner)).thenReturn(movedFile);

        ResponseEntity<VehicleFileResponseDTO> response = vehicleFileController.moveFile(10L, 400L, body, owner);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(50L, response.getBody().folderId());
    }

    @Test
    void moveFile_returnsOk_withNullFolderId_whenMovedToRoot() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);
        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);

        VehicleFile movedFile = new VehicleFile("manual.pdf", "application/pdf", "/uploads/uuid_manual.pdf", vehicle);
        movedFile.setId(400L);

        Map<String, Long> body = new HashMap<>();
        body.put("folderId", null);

        when(fileStorageService.moveFile(400L, 10L, null, owner)).thenReturn(movedFile);

        ResponseEntity<VehicleFileResponseDTO> response = vehicleFileController.moveFile(10L, 400L, body, owner);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNull(response.getBody().folderId());
    }

    @Test
    void moveFile_throwsForbidden_whenTargetFolderNotOwned() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        Map<String, Long> body = new HashMap<>();
        body.put("folderId", 999L);

        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN))
                .when(fileStorageService).moveFile(400L, 10L, 999L, owner);

        assertThrows(ResponseStatusException.class,
                () -> vehicleFileController.moveFile(10L, 400L, body, owner));
    }
}