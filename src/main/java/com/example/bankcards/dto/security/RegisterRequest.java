package com.example.bankcards.dto.security;

public record RegisterRequest(
        String username,
        String password,
        String email
) {
}
