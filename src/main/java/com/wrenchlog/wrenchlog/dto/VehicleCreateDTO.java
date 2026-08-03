package com.wrenchlog.wrenchlog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record VehicleCreateDTO(
        @NotBlank(message = "Make is required")
        String make,

        @NotBlank(message = "Model is required")
        String model,

        Integer year,

        @NotNull(message = "Kilometers is required")
        @PositiveOrZero(message = "Kilometers cannot be negative")
        Integer kilometers
) {}