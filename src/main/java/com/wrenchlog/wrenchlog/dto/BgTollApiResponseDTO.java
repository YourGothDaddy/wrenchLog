package com.wrenchlog.wrenchlog.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BgTollApiResponseDTO(
        BgTollVignetteDTO vignette,
        boolean ok
) {}