package com.wrenchlog.wrenchlog.dto;

import java.time.LocalDate;

public record VignetteCheckResponseDTO(
        boolean hasLocalReminder,
        LocalDate enteredExpiryDate,
        boolean bgTollFound,
        LocalDate bgTollExpiryDate,
        String bgTollStatus,
        boolean match,
        String message
) {}