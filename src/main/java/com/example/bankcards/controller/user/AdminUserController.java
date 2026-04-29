package com.example.bankcards.controller.user;

import com.example.bankcards.dto.user.CreateUserByAdminRequest;
import com.example.bankcards.dto.user.UserDto;
import com.example.bankcards.entity.enums.Role;
import com.example.bankcards.service.user.UserAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin Users", description = "Администрирование пользователей")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserAdminService userService;

    @GetMapping
    @Operation(summary = "Все пользователи")
    public ResponseEntity<Page<UserDto>> getAllUsers(
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        log.info("ADMIN: Запрос списка всех пользователей: page={}, size={}, sort={}",
                pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort()
        );

        return ResponseEntity.ok(userService.findAll(pageable));
    }

    @PostMapping
    @Operation(summary = "Создать пользователя")
    public ResponseEntity<UserDto> createUser(
            @Valid @RequestBody CreateUserByAdminRequest request
    ) {
        log.info("ADMIN: Запрос на создание пользователя администратором: username={}, email={}, role={}",
                request.username(), request.email(), request.role());

        UserDto created = userService.createUserByAdmin(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header("Location", "/api/admin/users/" + created.getId())
                .body(created);
    }

    @PutMapping("/{id}/role")
    @Operation(summary = "Изменить роль пользователя")
    public ResponseEntity<UserDto> changeUserRole(
            @PathVariable Long id,
            @RequestParam Role role
    ) {
        log.info("ADMIN: Запрос на смену роли пользователя: userId={}, новая роль={}", id, role);

        return ResponseEntity.ok(userService.changeRole(id, role));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить пользователя")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long id
    ) {
        log.info("ADMIN: Запрос на удаление пользователя: userId={}", id);

        userService.deleteUser(id);

        return ResponseEntity.noContent().build();
    }
}
