package com.wrenchlog.wrenchlog.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public record ElectricalSessionCreateDTO(
        @NotBlank(message = "Session label is required") String label,
        LocalDateTime sessionDate,
        String notes
) {}