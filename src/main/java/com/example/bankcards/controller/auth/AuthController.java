package com.example.bankcards.controller.auth;

import com.example.bankcards.dto.security.AuthRequest;
import com.example.bankcards.dto.security.AuthResponse;
import com.example.bankcards.dto.security.RegisterRequest;
import com.example.bankcards.service.auth.AuthService;
import com.example.bankcards.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST-контроллер для аутентификации и регистрации пользователей.
 *
 * <p>Все эндпоинты открыты — токен не требуется.</p>
 *
 * <p>Особенности:</p>
 * <ul>
 *   <li>При успешном входе возвращается JWT-токен</li>
 *   <li>Новые пользователи автоматически получают роль {@code USER}</li>
 *   <li>Пароли хешируются через {@code BCrypt}</li>
 *   <li>Username и email проверяются на уникальность</li>
 * </ul>
 *
 * @see AuthService
 * @see UserService
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Аутентификация и регистрация пользователей")
public class AuthController {
    private final AuthService authService;

    private final UserService userService;

    /**
     * Аутентифицирует пользователя и возвращает JWT-токен.
     *
     * <p>Проверяет связку username + пароль через {@link AuthService}.
     * При успехе генерирует токен, который нужно передавать в заголовке
     * {@code Authorization: Bearer <токен>} для доступа к защищённым эндпоинтам.</p>
     *
     * @param request DTO с полями {@code username} и {@code password}
     * @return {@code 200 OK} с {@link AuthResponse}, содержащим JWT-токен
     * @throws org.springframework.security.authentication.BadCredentialsException с {@code 401 UNAUTHORIZED} если учётные данные неверны
     */
    @PostMapping("/login")
    @Operation(
            summary = "Вход в систему",
            description = "Аутентификация пользователя по username и password. При успешном входе возвращается JWT токен."
    )
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        log.info("PUBLIC: Запрос на вход: username={}", request.username());

        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Регистрирует нового пользователя в системе.
     *
     * <p>Создаёт пользователя с ролью {@code USER}. Username и email должны быть
     * уникальными — иначе вернётся ошибка.</p>
     *
     * <p>Пароль хешируется через {@link org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder}
     * и никогда не хранится в открытом виде.</p>
     *
     * @param request DTO с полями username, password, email
     * @return {@code 201 Created} с сообщением об успешной регистрации
     * @throws com.example.bankcards.exception.AlreadyExistsException с {@code 409 CONFLICT} если username или email заняты
     */
    @PostMapping("/register")
    @Operation(
            summary = "Регистрация нового пользователя",
            description = "Создаёт нового пользователя с ролью USER. Username и email должны быть уникальными."
    )
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        log.info("PUBLIC: Запрос на регистрацию нового пользователя: username={}, email={}",
                request.username(), request.email()
        );

        userService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body("User registered successfully");
    }
}
