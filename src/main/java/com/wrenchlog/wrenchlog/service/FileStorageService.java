package com.wrenchlog.wrenchlog.service;

import com.wrenchlog.wrenchlog.dto.FileDownloadDto;
import com.wrenchlog.wrenchlog.model.Vehicle;
import com.wrenchlog.wrenchlog.model.VehicleFile;
import com.wrenchlog.wrenchlog.repository.VehicleFileRepository;
import com.wrenchlog.wrenchlog.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
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
            "image/webp"
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

        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("Invalid file type. Only PDFs and images are allowed.");
        }

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found with id: " + vehicleId));

        String originalFileName = file.getOriginalFilename();
        if(originalFileName == null || originalFileName.contains("..")){
            throw new RuntimeException("Invalid file name format.");
        }

        try{
            String uniqueFileName = UUID.randomUUID().toString() + "_" + originalFileName;

            Path targetLocation = this.fileStorageLocation.resolve(uniqueFileName);

            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            VehicleFile vehicleFile = new VehicleFile(originalFileName, targetLocation.toString(), file.getContentType(), vehicle);
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
}
