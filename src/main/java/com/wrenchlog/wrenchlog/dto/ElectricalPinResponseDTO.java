package com.wrenchlog.wrenchlog.dto;

public record ElectricalPinResponseDTO(
        Long id, String name, String expectedRange, int position
) {}