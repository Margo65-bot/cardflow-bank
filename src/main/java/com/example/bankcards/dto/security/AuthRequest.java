package com.example.bankcards.dto.security;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO для запроса на аутентификацию.
 *
 * @param username имя пользователя (логин)
 * @param password пароль
 */
public record AuthRequest(
        @NotBlank(message = "Имя пользователя обязательно")
        String username,

        @NotBlank(message = "Пароль обязателен")
        String password
) {}