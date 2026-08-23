package com.wrenchlog.wrenchlog.controller;

import com.wrenchlog.wrenchlog.dto.VehicleFolderCreateDTO;
import com.wrenchlog.wrenchlog.dto.VehicleFolderResponseDTO;
import com.wrenchlog.wrenchlog.model.User;
import com.wrenchlog.wrenchlog.model.Vehicle;
import com.wrenchlog.wrenchlog.model.VehicleFolder;
import com.wrenchlog.wrenchlog.repository.VehicleFileRepository;
import com.wrenchlog.wrenchlog.repository.VehicleFolderRepository;
import com.wrenchlog.wrenchlog.service.VehicleAccessService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles/{vehicleId}/folders")
public class VehicleFolderController {

    private final VehicleFolderRepository vehicleFolderRepository;
    private final VehicleFileRepository vehicleFileRepository;
    private final VehicleAccessService vehicleAccessService;

    public VehicleFolderController(VehicleFolderRepository vehicleFolderRepository,
                                   VehicleFileRepository vehicleFileRepository,
                                   VehicleAccessService vehicleAccessService) {
        this.vehicleFolderRepository = vehicleFolderRepository;
        this.vehicleFileRepository = vehicleFileRepository;
        this.vehicleAccessService = vehicleAccessService;
    }

    private VehicleFolderResponseDTO toResponseDTO(VehicleFolder folder) {
        long fileCount = vehicleFileRepository.findByVehicleIdAndFolderId(
                folder.getVehicle().getId(), folder.getId()
        ).size();

        return new VehicleFolderResponseDTO(
                folder.getId(), folder.getName(), folder.getVehicle().getId(),
                folder.getCreatedAt(), fileCount
        );
    }

    @PostMapping
    public ResponseEntity<VehicleFolderResponseDTO> createFolder(@PathVariable Long vehicleId,
                                                                 @Valid @RequestBody VehicleFolderCreateDTO dto,
                                                                 @AuthenticationPrincipal User user) {
        Vehicle vehicle = vehicleAccessService.getOwnedVehicleOrThrow(vehicleId, user);
        VehicleFolder folder = new VehicleFolder(dto.name(), vehicle);
        VehicleFolder saved = vehicleFolderRepository.save(folder);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponseDTO(saved));
    }

    @GetMapping
    public ResponseEntity<List<VehicleFolderResponseDTO>> getVehicleFolders(@PathVariable Long vehicleId,
                                                                            @AuthenticationPrincipal User user) {
        vehicleAccessService.getOwnedVehicleOrThrow(vehicleId, user);
        List<VehicleFolderResponseDTO> folders = vehicleFolderRepository.findByVehicleId(vehicleId)
                .stream().map(this::toResponseDTO).toList();
        return ResponseEntity.ok(folders);
    }

    @PutMapping("/{folderId}")
    public ResponseEntity<VehicleFolderResponseDTO> renameFolder(@PathVariable Long vehicleId,
                                                                 @PathVariable Long folderId,
                                                                 @Valid @RequestBody VehicleFolderCreateDTO dto,
                                                                 @AuthenticationPrincipal User user) {
        Vehicle vehicle = vehicleAccessService.getOwnedVehicleOrThrow(vehicleId, user);
        VehicleFolder folder = getOwnedFolderOrThrow(folderId, vehicle);

        folder.setName(dto.name());
        VehicleFolder saved = vehicleFolderRepository.save(folder);
        return ResponseEntity.ok(toResponseDTO(saved));
    }

    @DeleteMapping("/{folderId}")
    public ResponseEntity<Void> deleteFolder(@PathVariable Long vehicleId,
                                             @PathVariable Long folderId,
                                             @AuthenticationPrincipal User user) {
        Vehicle vehicle = vehicleAccessService.getOwnedVehicleOrThrow(vehicleId, user);
        VehicleFolder folder = getOwnedFolderOrThrow(folderId, vehicle);

        vehicleFolderRepository.delete(folder);
        return ResponseEntity.noContent().build();
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