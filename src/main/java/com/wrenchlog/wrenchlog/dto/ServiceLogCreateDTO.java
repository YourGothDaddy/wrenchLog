package com.wrenchlog.wrenchlog.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ServiceLogCreateDTO(
        @NotBlank(message = "Description is required")
        String description,

        @NotNull(message = "Cost is required")
        @DecimalMin(value = "0.0", message = "Cost cannot be negative")
        BigDecimal cost,

        @NotNull(message = "Kilometers at service is required")
        @PositiveOrZero(message = "Kilometers cannot be negative")
        Integer kilometersAtService,

        @NotNull(message = "Service date is required")
        LocalDate serviceDate
) {}