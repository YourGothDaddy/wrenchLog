package com.wrenchlog.wrenchlog.service;

import tools.jackson.databind.ObjectMapper;
import com.wrenchlog.wrenchlog.dto.RtaInspectionResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InspectionCheckService {
    private static final Logger log = LoggerFactory.getLogger(InspectionCheckService.class);

    private static final String CAPTCHA_URL = "https://rta.government.bg/services/check-inspection/checkinsp.php?captcha/inspection=1&rand=%d";
    private static final String SUBMIT_URL = "https://rta.government.bg/services/check-inspection/checkinsp.php";
    private static final DateTimeFormatter RTA_DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final long SESSION_TTL_MILLIS = 5 * 60 * 1000;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, PendingCheck> pendingChecks = new ConcurrentHashMap<>();

    public InspectionCheckService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public record CaptchaResult(String sessionToken, byte[] imageBytes) {}

    public record InspectionResult(boolean captchaInvalid, LocalDate inspectionExpiryDate, boolean valid) {}

    public CaptchaResult fetchCaptcha(String plateNumber) {
        evictExpiredSessions();

        String url = String.format(CAPTCHA_URL, (int) (Math.random() * 10000));

        ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.GET, HttpEntity.EMPTY, byte[].class);

        String phpSessId = extractPhpSessionId(response.getHeaders())
                .orElseThrow(() -> new RuntimeException("RTA did not return a session cookie"));

        String sessionToken = UUID.randomUUID().toString();
        pendingChecks.put(sessionToken, new PendingCheck(phpSessId, plateNumber, Instant.now()));

        byte[] imageBytes = response.getBody();
        if (imageBytes == null || imageBytes.length == 0) {
            throw new RuntimeException("RTA returned an empty captcha image");
        }

        return new CaptchaResult(sessionToken, imageBytes);
    }

    public InspectionResult submitCaptcha(String sessionToken, String captchaCode) {
        PendingCheck pending = pendingChecks.remove(sessionToken);
        if (pending == null) {
            throw new IllegalArgumentException("Session expired or not found. Please request a new captcha.");
        }

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("regNum", pending.plateNumber());
        formData.add("captcha", captchaCode);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.add(HttpHeaders.COOKIE, "PHPSESSID=" + pending.phpSessId());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);

        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(SUBMIT_URL, HttpMethod.POST, request, byte[].class);
            byte[] body = response.getBody();

            if (body == null) {
                return new InspectionResult(true, null, false);
            }

            RtaInspectionResponseDTO parsed;
            try {
                parsed = objectMapper.readValue(body, RtaInspectionResponseDTO.class);
            } catch (Exception jsonEx) {
                log.info("RTA response for plate {} was not valid JSON, treating as invalid captcha", pending.plateNumber());
                return new InspectionResult(true, null, false);
            }

            if (parsed.nextInspectionDate() == null || parsed.nextInspectionDate().isBlank()) {
                return new InspectionResult(false, null, parsed.isValid());
            }

            LocalDate expiryDate = LocalDate.parse(parsed.nextInspectionDate(), RTA_DATE_FORMAT);
            return new InspectionResult(false, expiryDate, parsed.isValid());

        } catch (RestClientException e) {
            log.warn("RTA inspection submit failed for plate {}: {}", pending.plateNumber(), e.getMessage());
            throw new RuntimeException("Could not reach RTA. Please try again later.", e);
        }
    }

    private Optional<String> extractPhpSessionId(HttpHeaders headers) {
        return headers.get(HttpHeaders.SET_COOKIE) == null ? Optional.empty() :
                headers.get(HttpHeaders.SET_COOKIE).stream()
                        .filter(c -> c.startsWith("PHPSESSID="))
                        .map(c -> c.split(";", 2)[0].substring("PHPSESSID=".length()))
                        .findFirst();
    }

    private void evictExpiredSessions() {
        Instant cutoff = Instant.now().minusMillis(SESSION_TTL_MILLIS);
        pendingChecks.entrySet().removeIf(entry -> entry.getValue().createdAt().isBefore(cutoff));
    }

    private record PendingCheck(String phpSessId, String plateNumber, Instant createdAt) {}
}