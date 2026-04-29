package com.example.bankcards.dto.security;

import jakarta.validation.constraints.NotBlank;

public record AuthRequest(
        @NotBlank(message = "Имя пользователя обязательно")
        String username,

        @NotBlank(message = "Пароль обязателен")
        String password
) {}