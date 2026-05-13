package com.example.bankcards.dto.security;

/**
 * DTO с ответом после успешной аутентификации.
 *
 * @param token JWT-токен для доступа к защищённым эндпоинтам.
 *              Формат: {@code Bearer <токен>}
 */
public record AuthResponse(
        String token
) {}
