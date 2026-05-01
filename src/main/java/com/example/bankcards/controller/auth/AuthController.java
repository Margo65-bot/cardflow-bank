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

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Аутентификация и регистрация пользователей")
public class AuthController {
    private final AuthService authService;

    private final UserService userService;

    @PostMapping("/login")
    @Operation(
            summary = "Вход в систему",
            description = "Аутентификация пользователя по username и password. При успешном входе возвращается JWT токен."
    )
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        log.info("PUBLIC: Запрос на вход: username={}", request.username());

        return ResponseEntity.ok(authService.login(request));
    }

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
