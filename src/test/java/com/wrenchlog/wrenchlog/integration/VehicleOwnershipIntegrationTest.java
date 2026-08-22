package com.wrenchlog.wrenchlog.integration;

import com.wrenchlog.wrenchlog.IntegrationTestBase;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class VehicleOwnershipIntegrationTest extends IntegrationTestBase {

    private static final String TEST_PASSWORD = "password123";

    private Cookie registerAndLogin(String username, String email) throws Exception {
        String registerBody = objectMapper.writeValueAsString(Map.of(
                "username", username, "email", email, "password", TEST_PASSWORD));
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated());

        String loginBody = objectMapper.writeValueAsString(Map.of(
                "username", username, "password", TEST_PASSWORD));
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn();

        return result.getResponse().getCookie("auth_token");
    }

    @Test
    void ownerCanCreateAndSeeTheirOwnVehicle() throws Exception {
        Cookie aliceCookie = registerAndLogin("alice_owner", "alice_owner@test.com");

        String vehicleBody = objectMapper.writeValueAsString(Map.of(
                "make", "Mercedes", "model", "E220", "year", 2015, "kilometers", 250000));

        mockMvc.perform(post("/api/vehicles")
                        .cookie(aliceCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vehicleBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.make").value("Mercedes"));

        mockMvc.perform(get("/api/vehicles").cookie(aliceCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].make").value("Mercedes"));
    }

    @Test
    void differentUser_cannotDeleteAnotherUsersVehicle() throws Exception {
        Cookie aliceCookie = registerAndLogin("alice_del", "alice_del@test.com");
        Cookie bobCookie = registerAndLogin("bob_del", "bob_del@test.com");

        String vehicleBody = objectMapper.writeValueAsString(Map.of(
                "make", "Toyota", "model", "Corolla", "year", 2020, "kilometers", 50000));

        MvcResult createResult = mockMvc.perform(post("/api/vehicles")
                        .cookie(aliceCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vehicleBody))
                .andExpect(status().isCreated())
                .andReturn();

        int vehicleId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asInt();

        mockMvc.perform(delete("/api/vehicles/" + vehicleId).cookie(bobCookie))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/vehicles").cookie(aliceCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(vehicleId));
    }

    @Test
    void differentUser_cannotViewAnotherUsersServiceLogs() throws Exception {
        Cookie aliceCookie = registerAndLogin("alice_logs", "alice_logs@test.com");
        Cookie bobCookie = registerAndLogin("bob_logs", "bob_logs@test.com");

        String vehicleBody = objectMapper.writeValueAsString(Map.of(
                "make", "Honda", "model", "Civic", "year", 2018, "kilometers", 80000));

        MvcResult createResult = mockMvc.perform(post("/api/vehicles")
                        .cookie(aliceCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vehicleBody))
                .andExpect(status().isCreated())
                .andReturn();

        int vehicleId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asInt();

        mockMvc.perform(get("/api/services?vehicleId=" + vehicleId).cookie(bobCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    void tamperedToken_isRejectedGracefully_notWith500() throws Exception {
        Cookie aliceCookie = registerAndLogin("alice_exp", "alice_exp@test.com");

        Cookie tamperedCookie = new Cookie("auth_token", aliceCookie.getValue() + "tampered");

        mockMvc.perform(get("/api/vehicles").cookie(tamperedCookie))
                .andExpect(status().isForbidden());
    }
}