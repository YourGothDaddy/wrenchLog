package com.wrenchlog.wrenchlog.dto;

import java.time.LocalDateTime;

public record VehicleFolderResponseDTO(
        Long id,
        String name,
        Long vehicleId,
        LocalDateTime createdAt,
        long fileCount
) {}