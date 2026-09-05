package com.wrenchlog.wrenchlog.controller;

import com.wrenchlog.wrenchlog.dto.LoginRequest;
import com.wrenchlog.wrenchlog.dto.LoginResponse;
import com.wrenchlog.wrenchlog.dto.RegisterRequest;
import com.wrenchlog.wrenchlog.model.User;
import com.wrenchlog.wrenchlog.security.JwtService;
import com.wrenchlog.wrenchlog.security.RefreshTokenService;
import com.wrenchlog.wrenchlog.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class UserController {

    private static final String ACCESS_COOKIE_NAME = "auth_token";
    private static final String REFRESH_COOKIE_NAME = "refresh_token";

    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpiration;

    public UserController(UserService userService, RefreshTokenService refreshTokenService, JwtService jwtService) {
        this.userService = userService;
        this.refreshTokenService = refreshTokenService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> registerUser(@Valid @RequestBody RegisterRequest request) {
        userService.registerNewUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = userService.loginUser(request);
        String refreshToken = refreshTokenService.issueToken(response.id());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildAccessCookie(response.token()).toString())
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(refreshToken).toString())
                .body(Map.of(
                        "id", response.id(),
                        "username", response.username(),
                        "email", response.email()
                ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request) {
        String rawRefreshToken = extractCookie(request, REFRESH_COOKIE_NAME);
        if (rawRefreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            RefreshTokenService.RotationResult result = refreshTokenService.rotate(rawRefreshToken);
            User user = userService.getUserById(result.userId());

            List<String> roles = user.getAuthorities().stream()
                    .map(grantedAuthority -> grantedAuthority.getAuthority())
                    .toList();
            String newAccessToken = jwtService.generateToken(user.getId(), user.getUsername(), roles);

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, buildAccessCookie(newAccessToken).toString())
                    .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(result.rawToken()).toString())
                    .build();
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header(HttpHeaders.SET_COOKIE, clearCookie(ACCESS_COOKIE_NAME, "/").toString())
                    .header(HttpHeaders.SET_COOKIE, clearCookie(REFRESH_COOKIE_NAME, "/api/auth").toString())
                    .build();
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String rawRefreshToken = extractCookie(request, REFRESH_COOKIE_NAME);
        if (rawRefreshToken != null) {
            refreshTokenService.revoke(rawRefreshToken);
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearCookie(ACCESS_COOKIE_NAME, "/").toString())
                .header(HttpHeaders.SET_COOKIE, clearCookie(REFRESH_COOKIE_NAME, "/api/auth").toString())
                .build();
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail()
        ));
    }

    private ResponseCookie buildAccessCookie(String token) {
        return ResponseCookie.from(ACCESS_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(jwtExpiration / 1000)
                .build();
    }

    private ResponseCookie buildRefreshCookie(String token) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(refreshTokenService.getRefreshExpirationMs() / 1000)
                .build();
    }

    private ResponseCookie clearCookie(String name, String path) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(path)
                .maxAge(0)
                .build();
    }

    private String extractCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}