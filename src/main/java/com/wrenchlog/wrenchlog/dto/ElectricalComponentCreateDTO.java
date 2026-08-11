package com.wrenchlog.wrenchlog.dto;

import jakarta.validation.constraints.NotBlank;

public record ElectricalComponentCreateDTO(
        @NotBlank(message = "Name is required") String name,
        String description
) {}