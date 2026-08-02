package com.wrenchlog.wrenchlog.controller;

import com.wrenchlog.wrenchlog.dto.LoginRequest;
import com.wrenchlog.wrenchlog.dto.LoginResponse;
import com.wrenchlog.wrenchlog.dto.RegisterRequest;
import com.wrenchlog.wrenchlog.model.User;
import com.wrenchlog.wrenchlog.service.UserService;
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
    private UserController userController;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        userController = new UserController(userService);
        ReflectionTestUtils.setField(userController, "jwtExpiration", 3600000L);
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
    void loginUser_returnsOkWithAuthCookie_whenCredentialsValid() {
        LoginRequest request = new LoginRequest("alice", "password123");
        LoginResponse serviceResponse = new LoginResponse(1L, "alice", "alice@test.com", "jwt-token-value");

        when(userService.loginUser(request)).thenReturn(serviceResponse);

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
        String cookie = cookies.getFirst();
        assertTrue(cookie.contains("auth_token=jwt-token-value"));
        assertTrue(cookie.contains("HttpOnly"));
        assertTrue(cookie.contains("Secure"));
        assertTrue(cookie.contains("SameSite=Strict"));
        assertTrue(cookie.contains("Max-Age=3600"));
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
    void logout_returnsOkWithExpiredCookie() {
        ResponseEntity<Void> response = userController.logout();

        assertEquals(HttpStatus.OK, response.getStatusCode());

        List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertNotNull(cookies);
        String cookie = cookies.getFirst();
        assertTrue(cookie.contains("auth_token="));
        assertTrue(cookie.contains("Max-Age=0"));
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