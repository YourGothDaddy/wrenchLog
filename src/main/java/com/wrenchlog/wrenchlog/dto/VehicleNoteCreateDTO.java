package com.wrenchlog.wrenchlog.dto;

import jakarta.validation.constraints.NotBlank;

public record VehicleNoteCreateDTO(
        @NotBlank(message = "Title is required")
        String title,

        @NotBlank(message = "Content is required")
        String content
) {}