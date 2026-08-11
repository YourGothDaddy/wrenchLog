package com.wrenchlog.wrenchlog.dto;

import java.util.List;

public record ElectricalComponentDetailDTO(
        Long id, String name, String description, Long vehicleId,
        List<ElectricalPinResponseDTO> pins,
        List<ElectricalSessionResponseDTO> sessions
) {}