package com.example.bankcards.dto.user;

import com.example.bankcards.entity.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO для создания пользователя через админскую панель.
 *
 * <p>В отличие от {@link com.example.bankcards.dto.security.RegisterRequest}, позволяет явно указать роль.
 * Доступен только пользователям с ролью {@code ADMIN}.</p>
 *
 * @param username имя пользователя (должно быть уникальным)
 * @param password пароль (будет захэширован)
 * @param email    email (должен быть уникальным и валидным)
 * @param role     роль пользователя ({@code USER} или {@code ADMIN})
 */
public record CreateUserByAdminRequest(
        @NotBlank(message = "Имя пользователя обязательно")
        String username,

        @NotBlank(message = "Пароль обязателен")
        String password,

        @NotBlank(message = "Email обязателен")
        @Email(message = "Введите корректный email адрес")
        String email,

        @NotNull(message = "Роль пользователя обязательна")
        Role role
) {}