package com.wrenchlog.wrenchlog.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record VehicleOdometerUpdateDTO(
        @NotNull(message = "Odometer value is required")
        @PositiveOrZero(message = "Odometer cannot be negative")
        Integer kilometers
) {}