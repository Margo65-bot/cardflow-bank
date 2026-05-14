package com.example.bankcards.service.auth.impl;

import com.example.bankcards.dto.security.AuthRequest;
import com.example.bankcards.dto.security.AuthResponse;
import com.example.bankcards.dto.user.UserDto;
import com.example.bankcards.service.auth.AuthService;
import com.example.bankcards.service.user.UserService;
import com.example.bankcards.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация {@link AuthService} с использованием Spring Security.
 *
 * <p>Процесс аутентификации:</p>
 * <ol>
 *   <li>Проверка учётных данных через {@link AuthenticationManager}</li>
 *   <li>Получение данных пользователя из БД</li>
 *   <li>Генерация JWT-токена с помощью {@link JwtUtil}</li>
 * </ol>
 *
 * <p>Все методы работают в режиме read-only транзакций,
 * так как не изменяют данные в БД.</p>
 *
 * @see JwtUtil
 * @see UserService
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {
    private final AuthenticationManager authenticationManager;

    private final JwtUtil jwtUtil;

    private final UserService userService;

    /**
     * Аутентифицирует пользователя и возвращает JWT-токен.
     *
     * <p>Алгоритм:</p>
     * <ol>
     *   <li>Создаёт {@link UsernamePasswordAuthenticationToken} из запроса</li>
     *   <li>Передаёт его в {@link AuthenticationManager} для проверки</li>
     *   <li>При успехе сохраняет аутентификацию в {@link SecurityContextHolder}</li>
     *   <li>Загружает {@link UserDto} из БД</li>
     *   <li>Генерирует токен с полями: userId, username, role</li>
     * </ol>
     *
     * @param request DTO с username и password
     * @return {@link AuthResponse} с JWT-токеном
     * @throws org.springframework.security.authentication.BadCredentialsException если учётные данные неверны
     * @throws com.example.bankcards.exception.NotFoundException если пользователь найден в Security, но отсутствует в БД
     */
    @Override
    public AuthResponse login(AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDto user = userService.findByUsername(request.username());
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole().name());
        return new AuthResponse(token);
    }
}
