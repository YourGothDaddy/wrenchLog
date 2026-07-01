package com.wrenchlog.wrenchlog.controller;

import com.wrenchlog.wrenchlog.dto.FileDownloadDto;
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
        FileDownloadDto downloadData = fileStorageService.loadFileAsResource(fileId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadData.fileName() + "\"")
                .contentType(MediaType.parseMediaType(downloadData.contentType()))
                .body(downloadData.resource());
    }
}