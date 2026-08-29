package com.wrenchlog.wrenchlog.dto;

import com.wrenchlog.wrenchlog.enums.ReminderSourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;

public record ServiceReminderCreateDTO(
        @NotBlank(message = "Title is required")
        String title,

        String description,

        @PositiveOrZero(message = "Last service odometer cannot be negative")
        Integer lastServiceAtOdometer,

        @PositiveOrZero(message = "Interval in odometer cannot be negative")
        Integer intervalOdometer,

        @PositiveOrZero(message = "Interval in months cannot be negative")
        Integer intervalMonths,

        LocalDate lastServiceAtDate,

        ReminderSourceType sourceType,

        LocalDate verifiedExpiryDate
) {}