package com.wrenchlog.wrenchlog.service;

import com.wrenchlog.wrenchlog.dto.LoginRequest;
import com.wrenchlog.wrenchlog.dto.LoginResponse;
import com.wrenchlog.wrenchlog.dto.RegisterRequest;
import com.wrenchlog.wrenchlog.model.User;
import com.wrenchlog.wrenchlog.repository.UserRepository;
import com.wrenchlog.wrenchlog.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtService = mock(JwtService.class);
        userService = new UserService(userRepository, passwordEncoder, jwtService);
    }

    @Test
    void registerNewUser_savesHashedPassword_whenUsernameAndEmailAreFree() {
        RegisterRequest request = new RegisterRequest("alice", "alice@test.com", "password123");

        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");

        userService.registerNewUser(request);

        verify(userRepository).save(argThat(user ->
                user.getUsername().equals("alice") &&
                        user.getEmail().equals("alice@test.com") &&
                        user.getPassword().equals("hashed-password")
        ));
    }

    @Test
    void registerNewUser_throwsIllegalArgumentException_whenUsernameTaken() {
        RegisterRequest request = new RegisterRequest("alice", "alice@test.com", "password123");

        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> userService.registerNewUser(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerNewUser_throwsIllegalArgumentException_whenEmailTaken() {
        RegisterRequest request = new RegisterRequest("alice", "alice@test.com", "password123");

        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@test.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> userService.registerNewUser(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void loginUser_returnsLoginResponse_whenCredentialsValid() {
        LoginRequest request = new LoginRequest("alice", "password123");

        User user = new User("alice", "alice@test.com", "hashed-password");
        user.setId(1L);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(true);
        when(jwtService.generateToken(eq(1L), eq("alice"), any())).thenReturn("jwt-token");

        LoginResponse response = userService.loginUser(request);

        assertEquals(1L, response.id());
        assertEquals("alice", response.username());
        assertEquals("jwt-token", response.token());
    }

    @Test
    void loginUser_throwsSecurityException_whenUsernameNotFound() {
        LoginRequest request = new LoginRequest("ghost", "password123");

        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(SecurityException.class, () -> userService.loginUser(request));
    }

    @Test
    void loginUser_throwsSecurityException_whenPasswordIncorrect() {
        LoginRequest request = new LoginRequest("alice", "wrongpassword");

        User user = new User("alice", "alice@test.com", "hashed-password");
        user.setId(1L);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "hashed-password")).thenReturn(false);

        assertThrows(SecurityException.class, () -> userService.loginUser(request));
        verify(jwtService, never()).generateToken(any(), any(), any());
    }
}