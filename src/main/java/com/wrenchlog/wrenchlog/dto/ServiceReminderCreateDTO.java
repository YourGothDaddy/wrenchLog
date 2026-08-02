package com.wrenchlog.wrenchlog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record ServiceReminderCreateDTO(
        @NotBlank(message = "Title is required")
        String title,

        String description,

        Integer lastServiceAtOdometer,

        @Positive(message = "Interval in odometer must be positive")
        Integer intervalOdometer,

        @Positive(message = "Interval in months must be positive")
        Integer intervalMonths,

        LocalDate lastServiceAtDate
) {}