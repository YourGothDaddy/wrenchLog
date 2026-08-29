package com.wrenchlog.wrenchlog.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RtaInspectionResponseDTO(
        String rvRegNum,
        String rvIdentNum,
        String nextInspectionDate,
        boolean isValid,
        boolean isPeriodic,
        Integer ecoCategory
) {}