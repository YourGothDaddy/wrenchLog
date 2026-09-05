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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserControllerTest {

    private UserService userService;
    private RefreshTokenService refreshTokenService;
    private JwtService jwtService;
    private UserController userController;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        refreshTokenService = mock(RefreshTokenService.class);
        jwtService = mock(JwtService.class);
        userController = new UserController(userService, refreshTokenService, jwtService);
        ReflectionTestUtils.setField(userController, "jwtExpiration", 3600000L);
        when(refreshTokenService.getRefreshExpirationMs()).thenReturn(604800000L);
    }

    private HttpServletRequest requestWithCookie(String name, String value) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        if (value == null) {
            when(request.getCookies()).thenReturn(null);
        } else {
            when(request.getCookies()).thenReturn(new Cookie[] { new Cookie(name, value) });
        }
        return request;
    }

    @Test
    void registerUser_returnsCreated_whenValid() {
        RegisterRequest request = new RegisterRequest("alice", "alice@test.com", "password123");

        ResponseEntity<Void> response = userController.registerUser(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(userService).registerNewUser(request);
    }

    @Test
    void registerUser_throwsIllegalArgumentException_whenUsernameTaken() {
        RegisterRequest request = new RegisterRequest("alice", "alice@test.com", "password123");

        doThrow(new IllegalArgumentException("Username 'alice' is already taken."))
                .when(userService).registerNewUser(request);

        assertThrows(IllegalArgumentException.class,
                () -> userController.registerUser(request));
    }

    @Test
    void loginUser_returnsOkWithAuthAndRefreshCookies_whenCredentialsValid() {
        LoginRequest request = new LoginRequest("alice", "password123");
        LoginResponse serviceResponse = new LoginResponse(1L, "alice", "alice@test.com", "jwt-token-value");

        when(userService.loginUser(request)).thenReturn(serviceResponse);
        when(refreshTokenService.issueToken(1L)).thenReturn("refresh-token-value");

        ResponseEntity<?> response = userController.loginUser(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(1L, body.get("id"));
        assertEquals("alice", body.get("username"));
        assertEquals("alice@test.com", body.get("email"));
        assertFalse(body.containsKey("token"));

        List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertNotNull(cookies);
        assertEquals(2, cookies.size());

        String accessCookie = cookies.stream().filter(c -> c.startsWith("auth_token=")).findFirst().orElseThrow();
        assertTrue(accessCookie.contains("auth_token=jwt-token-value"));
        assertTrue(accessCookie.contains("HttpOnly"));
        assertTrue(accessCookie.contains("Secure"));
        assertTrue(accessCookie.contains("SameSite=Strict"));
        assertTrue(accessCookie.contains("Max-Age=3600"));
        assertTrue(accessCookie.contains("Path=/"));

        String refreshCookie = cookies.stream().filter(c -> c.startsWith("refresh_token=")).findFirst().orElseThrow();
        assertTrue(refreshCookie.contains("refresh_token=refresh-token-value"));
        assertTrue(refreshCookie.contains("HttpOnly"));
        assertTrue(refreshCookie.contains("Secure"));
        assertTrue(refreshCookie.contains("SameSite=Strict"));
        assertTrue(refreshCookie.contains("Max-Age=604800"));
        assertTrue(refreshCookie.contains("Path=/api/auth"));
    }

    @Test
    void loginUser_throwsSecurityException_whenCredentialsInvalid() {
        LoginRequest request = new LoginRequest("alice", "wrongpassword");

        when(userService.loginUser(request))
                .thenThrow(new SecurityException("Invalid username or password"));

        assertThrows(SecurityException.class,
                () -> userController.loginUser(request));
    }

    @Test
    void refresh_returnsNewCookies_whenRefreshTokenValid() {
        HttpServletRequest request = requestWithCookie("refresh_token", "old-refresh-value");

        when(refreshTokenService.rotate("old-refresh-value"))
                .thenReturn(new RefreshTokenService.RotationResult(1L, "new-refresh-value"));

        User user = new User("alice", "alice@test.com", "hashed");
        user.setId(1L);
        when(userService.getUserById(1L)).thenReturn(user);
        when(jwtService.generateToken(eq(1L), eq("alice"), any())).thenReturn("new-access-token");

        ResponseEntity<?> response = userController.refresh(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertNotNull(cookies);
        assertEquals(2, cookies.size());

        String accessCookie = cookies.stream().filter(c -> c.startsWith("auth_token=")).findFirst().orElseThrow();
        assertTrue(accessCookie.contains("auth_token=new-access-token"));

        String refreshCookie = cookies.stream().filter(c -> c.startsWith("refresh_token=")).findFirst().orElseThrow();
        assertTrue(refreshCookie.contains("refresh_token=new-refresh-value"));
    }

    @Test
    void refresh_returnsUnauthorized_whenNoRefreshCookiePresent() {
        HttpServletRequest request = requestWithCookie("refresh_token", null);

        ResponseEntity<?> response = userController.refresh(request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verifyNoInteractions(refreshTokenService);
    }

    @Test
    void refresh_returnsUnauthorizedAndClearsCookies_whenRefreshTokenInvalid() {
        HttpServletRequest request = requestWithCookie("refresh_token", "reused-token");

        when(refreshTokenService.rotate("reused-token"))
                .thenThrow(new SecurityException("Refresh token reuse detected"));

        ResponseEntity<?> response = userController.refresh(request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

        List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertNotNull(cookies);
        assertTrue(cookies.stream().anyMatch(c -> c.startsWith("auth_token=") && c.contains("Max-Age=0")));
        assertTrue(cookies.stream().anyMatch(c -> c.startsWith("refresh_token=") && c.contains("Max-Age=0")));
    }

    @Test
    void logout_revokesRefreshTokenAndReturnsExpiredCookies() {
        HttpServletRequest request = requestWithCookie("refresh_token", "refresh-token-value");

        ResponseEntity<Void> response = userController.logout(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(refreshTokenService).revoke("refresh-token-value");

        List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertNotNull(cookies);
        assertTrue(cookies.stream().anyMatch(c -> c.startsWith("auth_token=") && c.contains("Max-Age=0")));
        assertTrue(cookies.stream().anyMatch(c -> c.startsWith("refresh_token=") && c.contains("Max-Age=0")));
    }

    @Test
    void logout_doesNotCallRevoke_whenNoRefreshCookiePresent() {
        HttpServletRequest request = requestWithCookie("refresh_token", null);

        ResponseEntity<Void> response = userController.logout(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(refreshTokenService, never()).revoke(any());
    }

    @Test
    void getCurrentUser_returnsUserData_whenAuthenticated() {
        User user = new User("alice", "alice@test.com", "hashed");
        user.setId(1L);

        ResponseEntity<?> response = userController.getCurrentUser(user);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(1L, body.get("id"));
        assertEquals("alice", body.get("username"));
        assertEquals("alice@test.com", body.get("email"));
    }

    @Test
    void getCurrentUser_returnsUnauthorized_whenNoUser() {
        ResponseEntity<?> response = userController.getCurrentUser(null);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
}