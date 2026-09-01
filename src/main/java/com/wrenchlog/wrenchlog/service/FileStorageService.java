package com.wrenchlog.wrenchlog.service;

import com.wrenchlog.wrenchlog.dto.FileDownloadDto;
import com.wrenchlog.wrenchlog.model.User;
import com.wrenchlog.wrenchlog.model.Vehicle;
import com.wrenchlog.wrenchlog.model.VehicleFile;
import com.wrenchlog.wrenchlog.model.VehicleFolder;
import com.wrenchlog.wrenchlog.repository.VehicleFileRepository;
import com.wrenchlog.wrenchlog.repository.VehicleFolderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {
    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private final Path fileStorageLocation;
    private final VehicleFileRepository vehicleFileRepository;
    private final VehicleFolderRepository vehicleFolderRepository;
    private final VehicleAccessService vehicleAccessService;

    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "application/pdf", "image/jpeg", "image/png", "image/webp",
            "image/vnd.dwg", "image/x-dwg", "application/acad",
            "model/vnd.dwf", "drawing/x-dwf",
            "model/vnd.dwfx+xps"
    );

    private static final List<String> ALLOWED_EXTENSIONS = List.of(".dwg", ".dwf", ".dwfx");

    public FileStorageService(@Value("${file.upload-dir}") String uploadDir,
                              VehicleFileRepository vehicleFileRepository,
                              VehicleFolderRepository vehicleFolderRepository,
                              VehicleAccessService vehicleAccessService){
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.vehicleFileRepository = vehicleFileRepository;
        this.vehicleFolderRepository = vehicleFolderRepository;
        this.vehicleAccessService = vehicleAccessService;

        try{
            Files.createDirectories(this.fileStorageLocation);
        }catch (IOException ex){
            throw new RuntimeException("Could not create the upload directory.", ex);
        }
    }

    public VehicleFile storeFile(MultipartFile file, Long vehicleId, Long folderId, User user){

        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot upload an empty file.");
        }

        String originalFileName = file.getOriginalFilename();

        if(originalFileName == null || originalFileName.contains("..")){
            throw new IllegalArgumentException("Invalid file name format.");
        }

        String contentType = file.getContentType();
        boolean isAllowedMime = contentType != null && ALLOWED_CONTENT_TYPES.contains(contentType);

        String fileExtension = "";
        int lastIndexOfDot = originalFileName.lastIndexOf(".");
        if (lastIndexOfDot != -1) {
            fileExtension = originalFileName.substring(lastIndexOfDot).toLowerCase();
        }
        boolean isAllowedExtension = ALLOWED_EXTENSIONS.contains(fileExtension);

        if (!isAllowedMime && !isAllowedExtension) {
            throw new IllegalArgumentException("Invalid file type. Only PDFs, images, and CAD diagrams (.dwg/.dwf) are allowed.");
        }

        Vehicle vehicle = vehicleAccessService.getOwnedVehicleOrThrow(vehicleId, user);
        VehicleFolder folder = folderId != null ? getOwnedFolderOrThrow(folderId, vehicle) : null;

        try{
            String uniqueFileName = UUID.randomUUID().toString() + "_" + originalFileName;
            Path targetLocation = this.fileStorageLocation.resolve(uniqueFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            String finalContentType = isAllowedMime ? contentType : "application/octet-stream";

            VehicleFile vehicleFile = new VehicleFile(
                    originalFileName, finalContentType, targetLocation.toString(), vehicle, folder
            );
            return vehicleFileRepository.save(vehicleFile);
        }catch (IOException ex){
            throw new RuntimeException("Could not store file. Please try again!", ex);
        }
    }

    public FileDownloadDto loadFileByIdOnly(Long fileId) {
        VehicleFile vehicleFile = vehicleFileRepository.findById(fileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));

        try {
            Path filePath = Paths.get(vehicleFile.getFilePath()).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new RuntimeException("Could not read the file from disk!");
            }

            return new FileDownloadDto(resource, vehicleFile.getFileType(), vehicleFile.getFileName());
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error reading file path", e);
        }
    }

    public boolean deleteFile(Long fileId, Long vehicleId, User user){
        VehicleFile vehicleFile = vehicleFileRepository.findById(fileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));

        if (!vehicleFile.getVehicle().getId().equals(vehicleId)) {
            throw new IllegalArgumentException("File does not belong to the specified vehicle.");
        }

        vehicleAccessService.assertOwnership(vehicleFile.getVehicle(), user);

        vehicleFileRepository.deleteById(fileId);

        try {
            Path filePath = Paths.get(vehicleFile.getFilePath());
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("Failed to delete physical file at {}: {}", vehicleFile.getFilePath(), e.getMessage());
        }

        return true;
    }

    public VehicleFile moveFile(Long fileId, Long vehicleId, Long folderId, User user){
        VehicleFile vehicleFile = vehicleFileRepository.findById(fileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));

        if (!vehicleFile.getVehicle().getId().equals(vehicleId)) {
            throw new IllegalArgumentException("File does not belong to the specified vehicle.");
        }

        Vehicle vehicle = vehicleAccessService.getOwnedVehicleOrThrow(vehicleId, user);
        VehicleFolder folder = folderId != null ? getOwnedFolderOrThrow(folderId, vehicle) : null;

        vehicleFile.setFolder(folder);
        return vehicleFileRepository.save(vehicleFile);
    }

    private VehicleFolder getOwnedFolderOrThrow(Long folderId, Vehicle vehicle) {
        VehicleFolder folder = vehicleFolderRepository.findById(folderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found"));

        if (!folder.getVehicle().getId().equals(vehicle.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Folder does not belong to this vehicle");
        }

        return folder;
    }
}