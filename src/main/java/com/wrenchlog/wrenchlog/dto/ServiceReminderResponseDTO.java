package com.wrenchlog.wrenchlog.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ServiceReminderResponseDTO(
        Long id,
        String title,
        String description,
        Integer intervalMonths,
        Integer intervalOdometer,
        LocalDate lastServiceAtDate,
        Integer lastServiceAtOdometer,
        LocalDateTime createdAt,
        Long vehicleId
) {}