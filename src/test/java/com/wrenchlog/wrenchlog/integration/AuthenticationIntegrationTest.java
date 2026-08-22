package com.wrenchlog.wrenchlog.integration;

import com.wrenchlog.wrenchlog.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthenticationIntegrationTest extends IntegrationTestBase {

    @Test
    void registerLoginMeLogout_fullFlow_worksEndToEnd() throws Exception {
        String registerBody = objectMapper.writeValueAsString(Map.of(
                "username", "alice", "email", "alice@test.com", "password", "password123"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated());

        String loginBody = objectMapper.writeValueAsString(Map.of(
                "username", "alice", "password", "password123"));

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andReturn();

        jakarta.servlet.http.Cookie authCookie = loginResult.getResponse().getCookie("auth_token");
        assertNotNull(authCookie);

        mockMvc.perform(get("/api/auth/me").cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"));

        mockMvc.perform(post("/api/auth/logout").cookie(authCookie))
                .andExpect(status().isOk());
    }

    @Test
    void login_returnsUnauthorized_whenPasswordWrong() throws Exception {
        String registerBody = objectMapper.writeValueAsString(Map.of(
                "username", "bob", "email", "bob@test.com", "password", "correctPassword1"));
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated());

        String loginBody = objectMapper.writeValueAsString(Map.of(
                "username", "bob", "password", "wrongPassword"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_returnsBadRequest_whenUsernameBlank() throws Exception {
        String registerBody = objectMapper.writeValueAsString(Map.of(
                "username", "", "email", "someone@test.com", "password", "password123"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_returnsBadRequest_whenUsernameAlreadyTaken() throws Exception {
        String registerBody = objectMapper.writeValueAsString(Map.of(
                "username", "charlie", "email", "charlie@test.com", "password", "password123"));
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated());

        String duplicateBody = objectMapper.writeValueAsString(Map.of(
                "username", "charlie", "email", "different@test.com", "password", "password456"));
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(duplicateBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void me_returnsForbidden_whenNoCookiePresent() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedEndpoint_returnsForbidden_whenNoCookiePresent() throws Exception {
        mockMvc.perform(get("/api/vehicles"))
                .andExpect(status().isForbidden());
    }
}