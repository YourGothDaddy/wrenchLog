package com.wrenchlog.wrenchlog.dto;

import java.time.LocalDateTime;

public record VehicleFileResponseDTO(
        Long id,
        String fileName,
        String fileType,
        LocalDateTime uploadDate,
        Long vehicleId,
        Long folderId
) {}