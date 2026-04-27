package com.example.bankcards.dto.security;

public record AuthRequest(
        String username,
        String password
) {
}
