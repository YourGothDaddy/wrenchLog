package com.wrenchlog.wrenchlog.dto;

import java.time.LocalDateTime;

public record VehicleNoteResponseDTO(
        Long id,
        String title,
        String content,
        LocalDateTime createdAt,
        Long vehicleId
) {}