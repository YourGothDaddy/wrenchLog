package com.wrenchlog.wrenchlog.dto;

public record LoginResponse(Long id, String username, String email, String token) {
}
