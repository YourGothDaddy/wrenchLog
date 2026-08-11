package com.wrenchlog.wrenchlog.dto;

public record ElectricalComponentResponseDTO(
        Long id, String name, String description, Long vehicleId
) {}