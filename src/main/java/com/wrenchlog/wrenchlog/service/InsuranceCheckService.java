package com.wrenchlog.wrenchlog.service;

import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class InsuranceCheckService {
    private static final Logger log = LoggerFactory.getLogger(InsuranceCheckService.class);

    private static final String CHECK_PAGE_URL =
            "https://www.guaranteefund.org/bg/информационен-център-и-справки/услуги/проверка-за-валидна-застраховка-грaждaнскa-отговорност-на-автомобилистите";
    private static final String CHALLENGE_URL = "https://www.guaranteefund.org/ajax/altchagenchlange.php?code=1";

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36";

    private static final DateTimeFormatter FORM_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORM_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter HTML_DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private static final Pattern SUCCESS_MARKER = Pattern.compile("success-results");
    private static final Pattern INSURER_PATTERN = Pattern.compile("insurers=[^\"]*\">([^<]+)</a>");
    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{2}\\.\\d{2}\\.\\d{4})г\\.\\s*\\d{2}:\\d{2}:\\d{2}ч\\.");

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public InsuranceCheckService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public record InsuranceResult(boolean found, String insurerName, LocalDate startDate, LocalDate endDate) {}

    private record AltchaChallenge(String algorithm, String challenge, long maxNumber, String salt, String signature) {}

    public InsuranceResult checkInsurance(String plateNumber) {
        Set<String> cookieJar = new LinkedHashSet<>();

        HttpHeaders pageHeaders = browserHeaders(null);
        ResponseEntity<String> pageResponse;
        try {
            pageResponse = restTemplate.exchange(CHECK_PAGE_URL, HttpMethod.GET, new HttpEntity<>(pageHeaders), String.class);
        } catch (RestClientException e) {
            log.warn("Guarantee Fund page fetch failed: {}", e.getMessage());
            throw new RuntimeException("Could not reach the Guarantee Fund. Please try again later.", e);
        }
        addCookies(cookieJar, pageResponse.getHeaders());

        HttpHeaders challengeHeaders = browserHeaders(CHECK_PAGE_URL);
        if (!cookieJar.isEmpty()) challengeHeaders.add(HttpHeaders.COOKIE, String.join("; ", cookieJar));

        long challengeStartedAt = System.currentTimeMillis();

        ResponseEntity<String> challengeResponse;
        try {
            challengeResponse = restTemplate.exchange(
                    CHALLENGE_URL, HttpMethod.GET, new HttpEntity<>(challengeHeaders), String.class);
        } catch (RestClientException e) {
            log.warn("Guarantee Fund challenge fetch failed: {}", e.getMessage());
            throw new RuntimeException("Could not reach the Guarantee Fund. Please try again later.", e);
        }
        addCookies(cookieJar, challengeResponse.getHeaders());

        AltchaChallenge challenge;
        try {
            challenge = objectMapper.readValue(challengeResponse.getBody(), AltchaChallenge.class);
        } catch (Exception e) {
            throw new RuntimeException("Could not parse Guarantee Fund challenge", e);
        }

        long solvedNumber = solve(challenge);
        long solveTookMillis = System.currentTimeMillis() - challengeStartedAt;
        String altchaPayload = buildPayload(challenge, solvedNumber, solveTookMillis);

        LocalDateTime now = LocalDateTime.now();
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("dkn", plateNumber);
        formData.add("rama", "");
        formData.add("stiker", "");
        formData.add("seria", "");
        formData.add("date", now.format(FORM_DATE_FORMAT));
        formData.add("datepickertime", now.format(FORM_TIME_FORMAT));
        formData.add("altcha_checkbox", "on");
        formData.add("altcha", altchaPayload);
        formData.add("send", "търси");

        HttpHeaders formHeaders = browserHeaders(CHECK_PAGE_URL);
        formHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        if (!cookieJar.isEmpty()) formHeaders.add(HttpHeaders.COOKIE, String.join("; ", cookieJar));

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, formHeaders);

        ResponseEntity<String> resultResponse;
        try {
            resultResponse = restTemplate.exchange(CHECK_PAGE_URL, HttpMethod.POST, request, String.class);
        } catch (RestClientException e) {
            log.warn("Guarantee Fund submit failed for plate {}: {}", plateNumber, e.getMessage());
            throw new RuntimeException("Could not reach the Guarantee Fund. Please try again later.", e);
        }

        return parseResult(resultResponse.getBody());
    }

    private HttpHeaders browserHeaders(String referer) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.USER_AGENT, USER_AGENT);
        headers.add(HttpHeaders.ACCEPT_LANGUAGE, "en-US,en;q=0.9,bg;q=0.8");
        if (referer != null) headers.add(HttpHeaders.REFERER, referer);
        return headers;
    }

    private void addCookies(Set<String> cookieJar, HttpHeaders headers) {
        List<String> setCookies = headers.get(HttpHeaders.SET_COOKIE);
        if (setCookies == null) return;
        for (String cookie : setCookies) {
            String nameValue = cookie.split(";", 2)[0];
            cookieJar.removeIf(existing -> existing.startsWith(nameValue.split("=", 2)[0] + "="));
            cookieJar.add(nameValue);
        }
    }

    private long solve(AltchaChallenge challenge) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (long n = 0; n <= challenge.maxNumber(); n++) {
                digest.reset();
                byte[] hash = digest.digest((challenge.salt() + n).getBytes(StandardCharsets.UTF_8));
                String hex = HexFormat.of().formatHex(hash);
                if (hex.equalsIgnoreCase(challenge.challenge())) {
                    return n;
                }
            }
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        throw new RuntimeException("Could not solve the Guarantee Fund captcha challenge");
    }

    private String buildPayload(AltchaChallenge challenge, long solvedNumber, long tookMillis) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("algorithm", challenge.algorithm());
        payload.put("challenge", challenge.challenge());
        payload.put("number", solvedNumber);
        payload.put("salt", challenge.salt());
        payload.put("signature", challenge.signature());
        payload.put("took", tookMillis);

        String json = objectMapper.writeValueAsString(payload);
        return java.util.Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    private InsuranceResult parseResult(String html) {
        if (html == null || !SUCCESS_MARKER.matcher(html).find()) {
            return new InsuranceResult(false, null, null, null);
        }

        List<String> insurers = new ArrayList<>();
        Matcher insurerMatcher = INSURER_PATTERN.matcher(html);
        while (insurerMatcher.find()) insurers.add(insurerMatcher.group(1));

        List<LocalDate> dates = new ArrayList<>();
        Matcher dateMatcher = DATE_PATTERN.matcher(html);
        while (dateMatcher.find()) dates.add(LocalDate.parse(dateMatcher.group(1), HTML_DATE_FORMAT));

        if (insurers.isEmpty() || dates.size() < 2) {
            return new InsuranceResult(false, null, null, null);
        }

        String bestInsurer = null;
        LocalDate bestStart = null;
        LocalDate bestEnd = null;

        for (int i = 0; i < insurers.size() && (2 * i + 1) < dates.size(); i++) {
            LocalDate start = dates.get(2 * i);
            LocalDate end = dates.get(2 * i + 1);
            if (bestEnd == null || end.isAfter(bestEnd)) {
                bestInsurer = insurers.get(i);
                bestStart = start;
                bestEnd = end;
            }
        }

        return new InsuranceResult(true, bestInsurer, bestStart, bestEnd);
    }
}