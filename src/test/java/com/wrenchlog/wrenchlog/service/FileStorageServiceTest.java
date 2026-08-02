package com.wrenchlog.wrenchlog.service;

import com.wrenchlog.wrenchlog.model.User;
import com.wrenchlog.wrenchlog.model.Vehicle;
import com.wrenchlog.wrenchlog.model.VehicleFile;
import com.wrenchlog.wrenchlog.repository.VehicleFileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FileStorageServiceTest {

    private VehicleFileRepository vehicleFileRepository;
    private VehicleAccessService vehicleAccessService;
    private FileStorageService fileStorageService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        vehicleFileRepository = mock(VehicleFileRepository.class);
        vehicleAccessService = mock(VehicleAccessService.class);
        fileStorageService = new FileStorageService(tempDir.toString(), vehicleFileRepository, vehicleAccessService);
    }

    @Test
    void storeFile_savesFileAndReturnsVehicleFile_whenValid() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);

        MultipartFile file = new MockMultipartFile("file", "manual.pdf", "application/pdf", "content".getBytes());

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, owner)).thenReturn(vehicle);
        when(vehicleFileRepository.save(any(VehicleFile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VehicleFile result = fileStorageService.storeFile(file, 10L, owner);

        assertEquals("manual.pdf", result.getFileName());
        assertEquals("application/pdf", result.getFileType());
        assertEquals(vehicle, result.getVehicle());
        verify(vehicleFileRepository).save(any(VehicleFile.class));
    }

    @Test
    void storeFile_throwsIllegalArgumentException_whenFileEmpty() {
        User owner = new User("alice", "alice@test.com", "hashed");
        MultipartFile emptyFile = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);

        assertThrows(IllegalArgumentException.class,
                () -> fileStorageService.storeFile(emptyFile, 10L, owner));
    }

    @Test
    void storeFile_throwsIllegalArgumentException_whenFileTypeNotAllowed() {
        User owner = new User("alice", "alice@test.com", "hashed");
        MultipartFile badFile = new MockMultipartFile("file", "virus.exe", "application/x-msdownload", "content".getBytes());

        assertThrows(IllegalArgumentException.class,
                () -> fileStorageService.storeFile(badFile, 10L, owner));
    }

    @Test
    void loadFileAsResource_returnsFile_whenOwnerAndFileExists() throws IOException {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);

        Path realFile = tempDir.resolve("manual.pdf");
        Files.writeString(realFile, "file contents");

        VehicleFile vehicleFile = new VehicleFile();
        vehicleFile.setId(400L);
        vehicleFile.setFileName("manual.pdf");
        vehicleFile.setFileType("application/pdf");
        vehicleFile.setFilePath(realFile.toString());
        vehicleFile.setVehicle(vehicle);

        when(vehicleFileRepository.findById(400L)).thenReturn(Optional.of(vehicleFile));

        var result = fileStorageService.loadFileAsResource(400L, owner);

        assertEquals("manual.pdf", result.fileName());
        assertEquals("application/pdf", result.contentType());
        verify(vehicleAccessService).assertOwnership(vehicle, owner);
    }

    @Test
    void loadFileAsResource_throwsForbidden_whenNotOwner() {
        User attacker = new User("bob", "bob@test.com", "hashed");
        attacker.setId(2L);

        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, new User());
        VehicleFile vehicleFile = new VehicleFile();
        vehicleFile.setId(400L);
        vehicleFile.setFilePath(tempDir.resolve("manual.pdf").toString());
        vehicleFile.setVehicle(vehicle);

        when(vehicleFileRepository.findById(400L)).thenReturn(Optional.of(vehicleFile));
        doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN))
                .when(vehicleAccessService).assertOwnership(vehicle, attacker);

        assertThrows(ResponseStatusException.class,
                () -> fileStorageService.loadFileAsResource(400L, attacker));
    }

    @Test
    void loadFileAsResource_throwsNotFound_whenFileMissingFromDatabase() {
        when(vehicleFileRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                () -> fileStorageService.loadFileAsResource(999L, new User()));
    }

    @Test
    void loadFileByIdOnly_returnsFile_regardlessOfOwnership() throws IOException {
        Path realFile = tempDir.resolve("manual.pdf");
        Files.writeString(realFile, "file contents");

        VehicleFile vehicleFile = new VehicleFile();
        vehicleFile.setId(400L);
        vehicleFile.setFileName("manual.pdf");
        vehicleFile.setFileType("application/pdf");
        vehicleFile.setFilePath(realFile.toString());

        when(vehicleFileRepository.findById(400L)).thenReturn(Optional.of(vehicleFile));

        var result = fileStorageService.loadFileByIdOnly(400L);

        assertEquals("manual.pdf", result.fileName());
        verifyNoInteractions(vehicleAccessService);
    }

    @Test
    void deleteFile_deletesSuccessfully_whenOwner() throws IOException {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);

        Path realFile = tempDir.resolve("manual.pdf");
        Files.writeString(realFile, "file contents");

        VehicleFile vehicleFile = new VehicleFile();
        vehicleFile.setId(400L);
        vehicleFile.setFilePath(realFile.toString());
        vehicleFile.setVehicle(vehicle);

        when(vehicleFileRepository.findById(400L)).thenReturn(Optional.of(vehicleFile));

        boolean result = fileStorageService.deleteFile(400L, 10L, owner);

        assertTrue(result);
        verify(vehicleFileRepository).deleteById(400L);
        assertFalse(Files.exists(realFile));
    }

    @Test
    void deleteFile_throwsIllegalArgumentException_whenVehicleMismatched() {
        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, new User());
        vehicle.setId(10L);

        VehicleFile vehicleFile = new VehicleFile();
        vehicleFile.setId(400L);
        vehicleFile.setVehicle(vehicle);

        when(vehicleFileRepository.findById(400L)).thenReturn(Optional.of(vehicleFile));

        assertThrows(IllegalArgumentException.class,
                () -> fileStorageService.deleteFile(400L, 999L, new User()));

        verify(vehicleFileRepository, never()).deleteById(any());
    }

    @Test
    void deleteFile_throwsForbidden_whenNotOwner() {
        User attacker = new User("bob", "bob@test.com", "hashed");
        attacker.setId(2L);

        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, new User());
        vehicle.setId(10L);

        VehicleFile vehicleFile = new VehicleFile();
        vehicleFile.setId(400L);
        vehicleFile.setVehicle(vehicle);

        when(vehicleFileRepository.findById(400L)).thenReturn(Optional.of(vehicleFile));
        doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN))
                .when(vehicleAccessService).assertOwnership(vehicle, attacker);

        assertThrows(ResponseStatusException.class,
                () -> fileStorageService.deleteFile(400L, 10L, attacker));

        verify(vehicleFileRepository, never()).deleteById(any());
    }

    @Test
    void deleteFile_throwsNotFound_whenFileMissing() {
        when(vehicleFileRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                () -> fileStorageService.deleteFile(999L, 10L, new User()));
    }

    @Test
    void deleteFile_returnsSuccess_whenPhysicalDeletionFails() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);

        VehicleFile vehicleFile = new VehicleFile();
        vehicleFile.setId(400L);
        vehicleFile.setFilePath("/uploads/test.pdf");
        vehicleFile.setVehicle(vehicle);

        when(vehicleFileRepository.findById(400L)).thenReturn(Optional.of(vehicleFile));

        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.deleteIfExists(any(Path.class)))
                    .thenThrow(new IOException("disk error"));

            assertDoesNotThrow(() -> fileStorageService.deleteFile(400L, 10L, owner));
        }

        verify(vehicleFileRepository).deleteById(400L);
    }
}