package com.wrenchlog.wrenchlog.controller;

import com.wrenchlog.wrenchlog.model.VehicleFile;
import com.wrenchlog.wrenchlog.repository.VehicleFileRepository;
import com.wrenchlog.wrenchlog.service.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/vehicles/{vehicleId}/files")
public class VehicleFileController {

    private final FileStorageService fileStorageService;
    private final VehicleFileRepository vehicleFileRepository;

    public VehicleFileController(FileStorageService fileStorageService, VehicleFileRepository vehicleFileRepository) {
        this.fileStorageService = fileStorageService;
        this.vehicleFileRepository = vehicleFileRepository;
    }

    @PostMapping
    public ResponseEntity<VehicleFile> uploadFile(@PathVariable Long vehicleId,
                                                  @RequestParam("file") MultipartFile file) {
        VehicleFile savedFile = fileStorageService.storeFile(file, vehicleId);
        return ResponseEntity.ok(savedFile);
    }

    @GetMapping
    public ResponseEntity<List<VehicleFile>> getVehicleFiles(@PathVariable Long vehicleId) {
        List<VehicleFile> files = vehicleFileRepository.findByVehicleId(vehicleId);
        return ResponseEntity.ok(files);
    }

    @GetMapping("/{fileId}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long vehicleId, @PathVariable Long fileId) {
        VehicleFile vehicleFile = vehicleFileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));

        try {
            Path filePath = Paths.get(vehicleFile.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + vehicleFile.getFileName() + "\"")
                        .contentType(MediaType.parseMediaType(vehicleFile.getFileType()))
                        .body(resource);
            } else {
                throw new RuntimeException("Could not read the file!");
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error reading file path", e);
        }
    }
}