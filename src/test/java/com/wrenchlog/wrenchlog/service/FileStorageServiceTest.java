package com.wrenchlog.wrenchlog.service;

import com.wrenchlog.wrenchlog.model.User;
import com.wrenchlog.wrenchlog.model.Vehicle;
import com.wrenchlog.wrenchlog.model.VehicleFile;
import com.wrenchlog.wrenchlog.model.VehicleFolder;
import com.wrenchlog.wrenchlog.repository.VehicleFileRepository;
import com.wrenchlog.wrenchlog.repository.VehicleFolderRepository;
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
    private VehicleFolderRepository vehicleFolderRepository;
    private VehicleAccessService vehicleAccessService;
    private FileStorageService fileStorageService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        vehicleFileRepository = mock(VehicleFileRepository.class);
        vehicleFolderRepository = mock(VehicleFolderRepository.class);
        vehicleAccessService = mock(VehicleAccessService.class);
        fileStorageService = new FileStorageService(tempDir.toString(), vehicleFileRepository, vehicleFolderRepository, vehicleAccessService);
    }

    @Test
    void storeFile_savesFileAndReturnsVehicleFile_whenValidAndNoFolder() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);

        MultipartFile file = new MockMultipartFile("file", "manual.pdf", "application/pdf", "content".getBytes());

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, owner)).thenReturn(vehicle);
        when(vehicleFileRepository.save(any(VehicleFile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VehicleFile result = fileStorageService.storeFile(file, 10L, null, owner);

        assertEquals("manual.pdf", result.getFileName());
        assertEquals("application/pdf", result.getFileType());
        assertEquals(vehicle, result.getVehicle());
        assertNull(result.getFolder());
        verify(vehicleFileRepository).save(any(VehicleFile.class));
        verifyNoInteractions(vehicleFolderRepository);
    }

    @Test
    void storeFile_savesFileIntoFolder_whenFolderIdProvidedAndOwned() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);

        VehicleFolder folder = new VehicleFolder("Manuals", vehicle);
        folder.setId(50L);

        MultipartFile file = new MockMultipartFile("file", "manual.pdf", "application/pdf", "content".getBytes());

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, owner)).thenReturn(vehicle);
        when(vehicleFolderRepository.findById(50L)).thenReturn(Optional.of(folder));
        when(vehicleFileRepository.save(any(VehicleFile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VehicleFile result = fileStorageService.storeFile(file, 10L, 50L, owner);

        assertEquals(folder, result.getFolder());
        verify(vehicleFileRepository).save(any(VehicleFile.class));
    }

    @Test
    void storeFile_throwsNotFound_whenFolderMissing() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);

        MultipartFile file = new MockMultipartFile("file", "manual.pdf", "application/pdf", "content".getBytes());

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, owner)).thenReturn(vehicle);
        when(vehicleFolderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                () -> fileStorageService.storeFile(file, 10L, 999L, owner));

        verify(vehicleFileRepository, never()).save(any());
    }

    @Test
    void storeFile_throwsForbidden_whenFolderBelongsToDifferentVehicle() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);

        Vehicle otherVehicle = new Vehicle("Honda", "Civic", 2018, 30000, owner);
        otherVehicle.setId(20L);

        VehicleFolder folder = new VehicleFolder("Manuals", otherVehicle);
        folder.setId(50L);

        MultipartFile file = new MockMultipartFile("file", "manual.pdf", "application/pdf", "content".getBytes());

        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, owner)).thenReturn(vehicle);
        when(vehicleFolderRepository.findById(50L)).thenReturn(Optional.of(folder));

        assertThrows(ResponseStatusException.class,
                () -> fileStorageService.storeFile(file, 10L, 50L, owner));

        verify(vehicleFileRepository, never()).save(any());
    }

    @Test
    void storeFile_throwsIllegalArgumentException_whenFileEmpty() {
        User owner = new User("alice", "alice@test.com", "hashed");
        MultipartFile emptyFile = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);

        assertThrows(IllegalArgumentException.class,
                () -> fileStorageService.storeFile(emptyFile, 10L, null, owner));
    }

    @Test
    void storeFile_throwsIllegalArgumentException_whenFileTypeNotAllowed() {
        User owner = new User("alice", "alice@test.com", "hashed");
        MultipartFile badFile = new MockMultipartFile("file", "virus.exe", "application/x-msdownload", "content".getBytes());

        assertThrows(IllegalArgumentException.class,
                () -> fileStorageService.storeFile(badFile, 10L, null, owner));
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

    @Test
    void moveFile_movesFileIntoFolder_whenOwnerAndFolderValid() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);

        VehicleFolder folder = new VehicleFolder("Manuals", vehicle);
        folder.setId(50L);

        VehicleFile vehicleFile = new VehicleFile();
        vehicleFile.setId(400L);
        vehicleFile.setVehicle(vehicle);

        when(vehicleFileRepository.findById(400L)).thenReturn(Optional.of(vehicleFile));
        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, owner)).thenReturn(vehicle);
        when(vehicleFolderRepository.findById(50L)).thenReturn(Optional.of(folder));
        when(vehicleFileRepository.save(any(VehicleFile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VehicleFile result = fileStorageService.moveFile(400L, 10L, 50L, owner);

        assertEquals(folder, result.getFolder());
        verify(vehicleFileRepository).save(vehicleFile);
    }

    @Test
    void moveFile_movesFileBackToRoot_whenFolderIdNull() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);

        VehicleFolder folder = new VehicleFolder("Manuals", vehicle);
        folder.setId(50L);

        VehicleFile vehicleFile = new VehicleFile();
        vehicleFile.setId(400L);
        vehicleFile.setVehicle(vehicle);
        vehicleFile.setFolder(folder);

        when(vehicleFileRepository.findById(400L)).thenReturn(Optional.of(vehicleFile));
        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, owner)).thenReturn(vehicle);
        when(vehicleFileRepository.save(any(VehicleFile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VehicleFile result = fileStorageService.moveFile(400L, 10L, null, owner);

        assertNull(result.getFolder());
        verifyNoInteractions(vehicleFolderRepository);
    }

    @Test
    void moveFile_throwsIllegalArgumentException_whenVehicleMismatched() {
        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, new User());
        vehicle.setId(10L);

        VehicleFile vehicleFile = new VehicleFile();
        vehicleFile.setId(400L);
        vehicleFile.setVehicle(vehicle);

        when(vehicleFileRepository.findById(400L)).thenReturn(Optional.of(vehicleFile));

        assertThrows(IllegalArgumentException.class,
                () -> fileStorageService.moveFile(400L, 999L, null, new User()));

        verify(vehicleFileRepository, never()).save(any());
    }

    @Test
    void moveFile_throwsNotFound_whenFileMissing() {
        when(vehicleFileRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                () -> fileStorageService.moveFile(999L, 10L, null, new User()));
    }

    @Test
    void moveFile_throwsForbidden_whenTargetFolderBelongsToDifferentVehicle() {
        User owner = new User("alice", "alice@test.com", "hashed");
        owner.setId(1L);

        Vehicle vehicle = new Vehicle("Toyota", "Corolla", 2020, 50000, owner);
        vehicle.setId(10L);

        Vehicle otherVehicle = new Vehicle("Honda", "Civic", 2018, 30000, owner);
        otherVehicle.setId(20L);

        VehicleFolder folder = new VehicleFolder("Manuals", otherVehicle);
        folder.setId(50L);

        VehicleFile vehicleFile = new VehicleFile();
        vehicleFile.setId(400L);
        vehicleFile.setVehicle(vehicle);

        when(vehicleFileRepository.findById(400L)).thenReturn(Optional.of(vehicleFile));
        when(vehicleAccessService.getOwnedVehicleOrThrow(10L, owner)).thenReturn(vehicle);
        when(vehicleFolderRepository.findById(50L)).thenReturn(Optional.of(folder));

        assertThrows(ResponseStatusException.class,
                () -> fileStorageService.moveFile(400L, 10L, 50L, owner));

        verify(vehicleFileRepository, never()).save(any());
    }
}