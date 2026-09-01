package com.wrenchlog.wrenchlog.dto;

import java.time.LocalDate;

public record InsuranceCheckResponseDTO(
        boolean hasLocalReminder,
        LocalDate enteredExpiryDate,
        boolean insurerFound,
        String insurerName,
        LocalDate insurerExpiryDate,
        boolean match,
        String message
) {}