package com.example.bankcards.dto.user;

import com.example.bankcards.entity.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

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