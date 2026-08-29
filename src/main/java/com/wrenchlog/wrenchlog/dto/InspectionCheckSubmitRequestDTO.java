package com.wrenchlog.wrenchlog.dto;

import jakarta.validation.constraints.NotBlank;

public record InspectionCheckSubmitRequestDTO(
        @NotBlank(message = "Session token is required")
        String sessionToken,

        @NotBlank(message = "Captcha code is required")
        String captchaCode
) {}