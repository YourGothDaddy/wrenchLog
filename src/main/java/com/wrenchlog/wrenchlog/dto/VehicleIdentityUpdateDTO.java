package com.wrenchlog.wrenchlog.dto;

import jakarta.validation.constraints.NotBlank;

public record VehicleIdentityUpdateDTO(
        @NotBlank(message = "Make is required")
        String make,

        @NotBlank(message = "Model is required")
        String model,

        Integer year
) {}