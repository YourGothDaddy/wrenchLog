package com.wrenchlog.wrenchlog.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ElectricalSessionResponseDTO(
        Long id, String label, LocalDateTime sessionDate, String notes,
        List<ElectricalReadingResponseDTO> readings
) {}