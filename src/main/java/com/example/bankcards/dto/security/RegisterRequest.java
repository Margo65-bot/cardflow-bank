package com.example.bankcards.dto.security;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO для запроса на регистрацию нового пользователя.
 *
 * <p>После успешной регистрации пользователь получает роль {@code USER}.</p>
 *
 * @param username имя пользователя (логин), должно быть уникальным
 * @param password пароль, будет захэширован через BCrypt
 * @param email    email, должен быть уникальным и валидным
 */
public record RegisterRequest(
        @NotBlank(message = "Имя пользователя обязательно")
        String username,

        @NotBlank(message = "Пароль обязателен")
        String password,

        @NotBlank(message = "Email обязателен")
        @Email(message = "Введите корректный email адрес")
        String email
) {}