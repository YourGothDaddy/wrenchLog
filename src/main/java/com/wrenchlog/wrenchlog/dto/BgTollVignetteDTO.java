package com.wrenchlog.wrenchlog.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BgTollVignetteDTO(
        String licensePlateNumber,
        LocalDateTime validityDateTo,
        String status,
        boolean statusBoolean
) {}