package com.wrenchlog.wrenchlog.dto;

public record InspectionCheckStartResponseDTO(
        String sessionToken,
        String captchaImageBase64
) {}