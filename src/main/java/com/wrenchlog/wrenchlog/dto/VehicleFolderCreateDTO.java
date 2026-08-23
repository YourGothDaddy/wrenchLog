package com.wrenchlog.wrenchlog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VehicleFolderCreateDTO(
        @NotBlank(message = "Folder name is required")
        @Size(max = 255, message = "Folder name must be 255 characters or fewer")
        String name
) {}