package com.wrenchlog.wrenchlog.service;

import com.wrenchlog.wrenchlog.dto.BgTollApiResponseDTO;
import com.wrenchlog.wrenchlog.dto.BgTollVignetteDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

@Service
public class BgTollService {
    private static final Logger log = LoggerFactory.getLogger(BgTollService.class);
    private static final String BGTOLL_URL_TEMPLATE = "https://check.bgtoll.bg/check/vignette/plate/BG/%s";

    private static final Map<Character, Character> CYRILLIC_TO_LATIN = Map.ofEntries(
            Map.entry('А', 'A'), Map.entry('В', 'B'), Map.entry('Е', 'E'),
            Map.entry('К', 'K'), Map.entry('М', 'M'), Map.entry('Н', 'H'),
            Map.entry('О', 'O'), Map.entry('Р', 'P'), Map.entry('С', 'C'),
            Map.entry('Т', 'T'), Map.entry('У', 'Y'), Map.entry('Х', 'X')
    );

    private final RestTemplate restTemplate;

    public BgTollService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Optional<BgTollVignetteDTO> lookupVignette(String plateNumber) {
        String sanitized = transliterate(plateNumber.toUpperCase()).replaceAll("[^A-Z0-9]", "");
        if (sanitized.isEmpty()) {
            return Optional.empty();
        }

        String url = String.format(BGTOLL_URL_TEMPLATE, URLEncoder.encode(sanitized, StandardCharsets.UTF_8));

        try {
            BgTollApiResponseDTO response = restTemplate.getForObject(url, BgTollApiResponseDTO.class);
            if (response == null || !response.ok() || response.vignette() == null) {
                return Optional.empty();
            }
            return Optional.of(response.vignette());
        } catch (RestClientException e) {
            log.warn("BGTOLL lookup failed for plate {}: {}", sanitized, e.getMessage());
            return Optional.empty();
        }
    }

    private String transliterate(String input) {
        StringBuilder result = new StringBuilder(input.length());
        for (char c : input.toCharArray()) {
            result.append(CYRILLIC_TO_LATIN.getOrDefault(c, c));
        }
        return result.toString();
    }
}