package com.wrenchlog.wrenchlog.service;

import com.wrenchlog.wrenchlog.dto.BgTollApiResponseDTO;
import com.wrenchlog.wrenchlog.dto.BgTollVignetteDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BgTollServiceTest {

    private RestTemplate restTemplate;
    private BgTollService bgTollService;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        bgTollService = new BgTollService(restTemplate);
    }

    @Test
    void lookupVignette_returnsVignette_whenApiRespondsOk() {
        BgTollVignetteDTO vignette = new BgTollVignetteDTO(
                "CA1234BC", LocalDateTime.of(2026, 12, 31, 0, 0), "Активна", true);
        BgTollApiResponseDTO response = new BgTollApiResponseDTO(vignette, true);

        when(restTemplate.getForObject(any(String.class), eq(BgTollApiResponseDTO.class)))
                .thenReturn(response);

        Optional<BgTollVignetteDTO> result = bgTollService.lookupVignette("CA1234BC");

        assertTrue(result.isPresent());
        assertEquals("CA1234BC", result.get().licensePlateNumber());
    }

    @Test
    void lookupVignette_returnsEmpty_whenApiReturnsOkFalse() {
        BgTollVignetteDTO vignette = new BgTollVignetteDTO(
                "CA1234BC", LocalDateTime.of(2026, 12, 31, 0, 0), "Активна", true);
        BgTollApiResponseDTO response = new BgTollApiResponseDTO(vignette, false);

        when(restTemplate.getForObject(any(String.class), eq(BgTollApiResponseDTO.class)))
                .thenReturn(response);

        Optional<BgTollVignetteDTO> result = bgTollService.lookupVignette("CA1234BC");

        assertTrue(result.isEmpty());
    }

    @Test
    void lookupVignette_returnsEmpty_whenVignetteFieldIsNull() {
        BgTollApiResponseDTO response = new BgTollApiResponseDTO(null, true);

        when(restTemplate.getForObject(any(String.class), eq(BgTollApiResponseDTO.class)))
                .thenReturn(response);

        Optional<BgTollVignetteDTO> result = bgTollService.lookupVignette("CA1234BC");

        assertTrue(result.isEmpty());
    }

    @Test
    void lookupVignette_returnsEmpty_whenResponseBodyIsNull() {
        when(restTemplate.getForObject(any(String.class), eq(BgTollApiResponseDTO.class)))
                .thenReturn(null);

        Optional<BgTollVignetteDTO> result = bgTollService.lookupVignette("CA1234BC");

        assertTrue(result.isEmpty());
    }

    @Test
    void lookupVignette_returnsEmpty_whenRestClientExceptionThrown() {
        when(restTemplate.getForObject(any(String.class), eq(BgTollApiResponseDTO.class)))
                .thenThrow(new RestClientException("timeout"));

        Optional<BgTollVignetteDTO> result = bgTollService.lookupVignette("CA1234BC");

        assertTrue(result.isEmpty());
        verify(restTemplate).getForObject(any(String.class), eq(BgTollApiResponseDTO.class));
    }

    @Test
    void lookupVignette_returnsEmpty_withoutCallingApi_whenPlateIsBlank() {
        Optional<BgTollVignetteDTO> result = bgTollService.lookupVignette("   ");

        assertTrue(result.isEmpty());
        verifyNoInteractions(restTemplate);
    }

    @Test
    void lookupVignette_returnsEmpty_withoutCallingApi_whenPlateHasOnlySymbols() {
        Optional<BgTollVignetteDTO> result = bgTollService.lookupVignette("---!!!");

        assertTrue(result.isEmpty());
        verifyNoInteractions(restTemplate);
    }

    @Test
    void lookupVignette_transliteratesCyrillicPlate_beforeCallingApi() {
        BgTollVignetteDTO vignette = new BgTollVignetteDTO(
                "CA1234BC", LocalDateTime.of(2026, 12, 31, 0, 0), "Активна", true);
        BgTollApiResponseDTO response = new BgTollApiResponseDTO(vignette, true);

        when(restTemplate.getForObject(any(String.class), eq(BgTollApiResponseDTO.class)))
                .thenReturn(response);

        bgTollService.lookupVignette("СА1234ВС");

        verify(restTemplate).getForObject(
                eq("https://check.bgtoll.bg/check/vignette/plate/BG/CA1234BC"),
                eq(BgTollApiResponseDTO.class)
        );
    }
}