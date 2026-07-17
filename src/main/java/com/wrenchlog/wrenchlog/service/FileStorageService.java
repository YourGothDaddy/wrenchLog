package com.wrenchlog.wrenchlog.service;

import com.wrenchlog.wrenchlog.dto.FileDownloadDto;
import com.wrenchlog.wrenchlog.model.Vehicle;
import com.wrenchlog.wrenchlog.model.VehicleFile;
import com.wrenchlog.wrenchlog.repository.VehicleFileRepository;
import com.wrenchlog.wrenchlog.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
    private final Path fileStorageLocation;
    private final VehicleFileRepository vehicleFileRepository;
    private final VehicleRepository vehicleRepository;

    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/vnd.dwg",
            "image/x-dwg",
            "application/acad",
            "model/vnd.dwf",
            "drawing/x-dwf"
    );

    private static final List<String> ALLOWED_EXTENSIONS = List.of(
            ".dwg",
            ".dwf"
    );

    public FileStorageService(@Value("${file.upload-dir}") String uploadDir,
                              VehicleFileRepository vehicleFileRepository,
                              VehicleRepository vehicleRepository){
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.vehicleFileRepository = vehicleFileRepository;
        this.vehicleRepository = vehicleRepository;

        try{
            Files.createDirectories(this.fileStorageLocation);
        }catch (IOException ex){
            throw new RuntimeException("Could not create the upload directory.", ex);
        }
    }

    public VehicleFile storeFile(MultipartFile file, Long vehicleId){

        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot upload an empty file.");
        }

        String originalFileName = file.getOriginalFilename();

        if(originalFileName == null || originalFileName.contains("..")){
            throw new RuntimeException("Invalid file name format.");
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

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found with id: " + vehicleId));

        try{
            String uniqueFileName = UUID.randomUUID().toString() + "_" + originalFileName;

            Path targetLocation = this.fileStorageLocation.resolve(uniqueFileName);

            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            String finalContentType = isAllowedMime ? contentType : "application/octet-stream";

            VehicleFile vehicleFile = new VehicleFile(
                    originalFileName,
                    finalContentType,
                    targetLocation.toString(),
                    vehicle
            );
            return vehicleFileRepository.save(vehicleFile);
        }catch (IOException ex){
            throw new RuntimeException("Could not store file. Please try again!", ex);
        }
    }

    public FileDownloadDto loadFileAsResource(Long fileId) {
        VehicleFile vehicleFile = vehicleFileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found in database"));

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

    public boolean deleteFile(Long fileId, Long vehicleId, String userId){
        VehicleFile vehicleFile = vehicleFileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found in database"));

        if (!vehicleFile.getVehicle().getId().equals(vehicleId)) {
            throw new IllegalArgumentException("Malicious request: File does not belong to the specified vehicle.");
        }

        if (!vehicleFile.getVehicle().getUser().getUsername().equals(userId)) {
            throw new SecurityException("Access Denied: You do not own this vehicle.");
        }

        try {
            Path filePath = Paths.get(vehicleFile.getFilePath());
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Could not delete physical file: " + e.getMessage());
        }

        vehicleFileRepository.deleteById(fileId);
        return true;
    }
}
