package com.wrenchlog.wrenchlog.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ServiceLogResponseDTO(
        Long id,
        String description,
        BigDecimal cost,
        int kilometersAtService,
        LocalDate serviceDate,
        Long vehicleId
) {}