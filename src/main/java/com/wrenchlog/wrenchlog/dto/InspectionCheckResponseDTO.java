package com.wrenchlog.wrenchlog.dto;

import java.time.LocalDate;

public record InspectionCheckResponseDTO(
        boolean hasLocalReminder,
        LocalDate enteredExpiryDate,
        boolean rtaFound,
        LocalDate rtaExpiryDate,
        boolean match,
        boolean captchaInvalid,
        String message
) {}