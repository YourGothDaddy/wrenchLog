package com.wrenchlog.wrenchlog.dto;

import jakarta.validation.constraints.NotBlank;

public record ElectricalPinCreateDTO(
        @NotBlank(message = "Pin name is required") String name,
        String expectedRange
) {}