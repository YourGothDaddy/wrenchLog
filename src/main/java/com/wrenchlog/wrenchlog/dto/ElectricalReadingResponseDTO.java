package com.wrenchlog.wrenchlog.dto;

public record ElectricalReadingResponseDTO(
        Long pinId, String value, String unit
) {}