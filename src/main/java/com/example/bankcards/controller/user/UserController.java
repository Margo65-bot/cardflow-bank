package com.example.bankcards.controller.user;

import com.example.bankcards.dto.user.UserDto;
import com.example.bankcards.security.CustomUserDetails;
import com.example.bankcards.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST-контроллер для операций с профилем текущего пользователя.
 *
 * <p><b>Доступ:</b> любой авторизованный пользователь (роли USER и ADMIN).</p>
 *
 * <p>Позволяет получить данные своего профиля.</p>
 *
 * @see UserService
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Операции с пользователями")
@SecurityRequirement(name = "bearerAuth")
public class UserController {
    private final UserService userService;

    /**
     * Возвращает профиль текущего авторизованного пользователя.
     *
     * <p>Доступен всем авторизованным пользователям независимо от роли.</p>
     *
     * @param userDetails данные текущего пользователя из JWT-токена
     * @return {@code 200 OK} с {@link UserDto}
     * @throws com.example.bankcards.exception.NotFoundException с {@code 404 NOT FOUND} если пользователь не найден
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Мой профиль", description = "Доступен всем авторизованным пользователям")
    public ResponseEntity<UserDto> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUserId();

        log.info("USER: Запрос профиля пользователя: userId={}, username={}", userId, userDetails.getUsername());

        return ResponseEntity.ok(userService.findById(userId));
    }
}
