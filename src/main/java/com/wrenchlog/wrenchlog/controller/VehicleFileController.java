package com.wrenchlog.wrenchlog.controller;

import com.wrenchlog.wrenchlog.dto.FileDownloadDto;
import com.wrenchlog.wrenchlog.model.User;
import com.wrenchlog.wrenchlog.model.VehicleFile;
import com.wrenchlog.wrenchlog.repository.VehicleFileRepository;
import com.wrenchlog.wrenchlog.security.JwtService;
import com.wrenchlog.wrenchlog.service.FileStorageService;
import com.wrenchlog.wrenchlog.service.VehicleAccessService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vehicles/{vehicleId}/files")
public class VehicleFileController {

    private final FileStorageService fileStorageService;
    private final VehicleFileRepository vehicleFileRepository;
    private final VehicleAccessService vehicleAccessService;
    private final JwtService jwtService;

    public VehicleFileController(FileStorageService fileStorageService,
                                 VehicleFileRepository vehicleFileRepository,
                                 VehicleAccessService vehicleAccessService,
                                 JwtService jwtService) {
        this.fileStorageService = fileStorageService;
        this.vehicleFileRepository = vehicleFileRepository;
        this.vehicleAccessService = vehicleAccessService;
        this.jwtService = jwtService;
    }

    @PostMapping
    public ResponseEntity<VehicleFile> uploadFile(@PathVariable Long vehicleId,
                                                  @RequestParam("file") MultipartFile file,
                                                  @AuthenticationPrincipal User user) {
        VehicleFile savedFile = fileStorageService.storeFile(file, vehicleId, user);
        return ResponseEntity.ok(savedFile);
    }

    @GetMapping
    public ResponseEntity<List<VehicleFile>> getVehicleFiles(@PathVariable Long vehicleId,
                                                             @AuthenticationPrincipal User user) {
        vehicleAccessService.getOwnedVehicleOrThrow(vehicleId, user);
        return ResponseEntity.ok(vehicleFileRepository.findByVehicleId(vehicleId));
    }

    @GetMapping("/{fileId}/download")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable Long vehicleId,
            @PathVariable Long fileId,
            @RequestParam String token
    ) {
        try {
            jwtService.validateAndExtractDownloadToken(token, fileId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        FileDownloadDto downloadData = fileStorageService.loadFileByIdOnly(fileId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadData.fileName() + "\"")
                .contentType(MediaType.parseMediaType(downloadData.contentType()))
                .body(downloadData.resource());
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> deleteFile(@PathVariable Long vehicleId,
                                           @PathVariable Long fileId,
                                           @AuthenticationPrincipal User user) {
        fileStorageService.deleteFile(fileId, vehicleId, user);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/{fileId}/download-token")
    public ResponseEntity<Map<String, String>> getDownloadToken(
            @PathVariable Long vehicleId,
            @PathVariable Long fileId,
            @AuthenticationPrincipal User user
    ) {
        vehicleAccessService.getOwnedVehicleOrThrow(vehicleId, user);

        String token = jwtService.generateFileDownloadToken(fileId, user.getId());
        return ResponseEntity.ok(Map.of("token", token));
    }
}
