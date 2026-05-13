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

/**
 * REST-контроллер для управления пользователями от имени администратора.
 *
 * <p><b>Доступ:</b> только пользователи с ролью {@code ADMIN}.</p>
 *
 * <p>Предоставляет полный CRUD над пользователями:</p>
 * <ul>
 *   <li>Просмотр всех пользователей с пагинацией</li>
 *   <li>Создание пользователя с указанием роли</li>
 *   <li>Изменение роли пользователя</li>
 *   <li>Удаление пользователя</li>
 * </ul>
 *
 * @see UserAdminService
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin Users", description = "Администрирование пользователей")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {
    private final UserAdminService userService;

    /**
     * Возвращает список всех пользователей с пагинацией.
     *
     * <p>Сортировка по умолчанию: по ID, по возрастанию.</p>
     *
     * @param pageable параметры пагинации
     * @return {@code 200 OK} со страницей {@link UserDto}
     */
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

    /**
     * Создаёт нового пользователя с указанной ролью.
     *
     * <p>В отличие от публичной регистрации, позволяет сразу назначить роль
     * {@code ADMIN} или {@code USER}. Username и email должны быть уникальными.</p>
     *
     * @param request DTO с данными пользователя (username, password, email, role)
     * @return {@code 201 Created} с заголовком {@code Location} и созданным пользователем
     * @throws com.example.bankcards.exception.AlreadyExistsException  с {@code 409 CONFLICT} если username или email заняты
     */
    @PostMapping
    @Operation(summary = "Создать пользователя")
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody CreateUserByAdminRequest request) {
        log.info("ADMIN: Запрос на создание пользователя администратором: username={}, email={}, role={}",
                request.username(), request.email(), request.role()
        );

        UserDto created = userService.createUserByAdmin(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header("Location", "/api/admin/users/" + created.getId())
                .body(created);
    }

    /**
     * Изменяет роль существующего пользователя.
     *
     * <p>Можно повысить до {@code ADMIN} или понизить до {@code USER}.</p>
     *
     * @param id   идентификатор пользователя
     * @param role новая роль ({@code USER} или {@code ADMIN})
     * @return {@code 200 OK} с обновлёнными данными пользователя
     * @throws com.example.bankcards.exception.NotFoundException  с {@code 404 NOT FOUND} если пользователь не найден
     * @throws com.example.bankcards.exception.AccessDeniedException с {@code 403 FORBIDDEN} если у пользователя есть карты при присваивании роли ADMIN
     */
    @PutMapping("/{id}/role")
    @Operation(summary = "Изменить роль пользователя")
    public ResponseEntity<UserDto> changeUserRole(
            @PathVariable Long id,
            @RequestParam Role role
    ) {
        log.info("ADMIN: Запрос на смену роли пользователя: userId={}, новая роль={}", id, role);

        return ResponseEntity.ok(userService.changeRole(id, role));
    }

    /**
     * Удаляет пользователя из системы.
     *
     * <p><b>Внимание:</b> операция необратима. Все карты и транзакции пользователя
     * также будут удалены (каскадное удаление).</p>
     *
     * @param id идентификатор пользователя
     * @return {@code 204 No Content} при успешном удалении
     * @throws com.example.bankcards.exception.NotFoundException с {@code 404 NOT FOUND} если пользователь не найден
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить пользователя")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        log.info("ADMIN: Запрос на удаление пользователя: userId={}", id);

        userService.deleteUser(id);

        return ResponseEntity.noContent().build();
    }
}
