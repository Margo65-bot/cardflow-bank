package com.example.bankcards.service.auth;

import com.example.bankcards.dto.security.AuthRequest;
import com.example.bankcards.dto.security.AuthResponse;

/**
 * Сервис аутентификации пользователей.
 *
 * <p>Отвечает за проверку учётных данных и генерацию JWT-токенов.</p>
 *
 * @see com.example.bankcards.service.auth.impl.AuthServiceImpl
 */
public interface AuthService {
    /**
     * Выполняет вход пользователя в систему.
     *
     * <p>Проверяет связку username + пароль через {@link org.springframework.security.authentication.AuthenticationManager}.
     * При успехе генерирует JWT-токен с информацией о пользователе.</p>
     *
     * @param request учётные данные (username и пароль)
     * @return ответ с JWT-токеном
     * @throws org.springframework.security.authentication.BadCredentialsException если учётные данные неверны
     */
    AuthResponse login(AuthRequest request);
}