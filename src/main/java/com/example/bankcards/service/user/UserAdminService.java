package com.example.bankcards.service.user;

import com.example.bankcards.dto.user.CreateUserByAdminRequest;
import com.example.bankcards.dto.user.UserDto;
import com.example.bankcards.entity.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Сервис для управления пользователями от имени администратора.
 *
 * <p>Предоставляет полный CRUD над пользователями, включая смену ролей.
 */
public interface UserAdminService {
    /**
     * Создаёт пользователя с указанной ролью.
     *
     * @param request данные пользователя (username, password, email, role)
     * @return созданный пользователь
     * @throws com.example.bankcards.exception.AlreadyExistsException если username или email заняты
     */
    UserDto createUserByAdmin(CreateUserByAdminRequest request);

    /**
     * Возвращает всех пользователей с пагинацией.
     *
     * @param pageable параметры пагинации
     * @return страница с пользователями
     */
    Page<UserDto> findAll(Pageable pageable);

    /**
     * Изменяет роль пользователя.
     *
     * <p>Нельзя назначить роль {@code ADMIN} пользователю, у которого есть карты.</p>
     *
     * @param userId идентификатор пользователя
     * @param role   новая роль
     * @return обновлённый пользователь
     * @throws com.example.bankcards.exception.NotFoundException если пользователь не найден
     * @throws com.example.bankcards.exception.AccessDeniedException если у пользователя есть карты и назначается роль ADMIN
     */
    UserDto changeRole(Long userId, Role role);

    /**
     * Удаляет пользователя.
     *
     * <p>Карты и транзакции удаляются каскадно.</p>
     *
     * @param userId идентификатор пользователя
     * @throws com.example.bankcards.exception.NotFoundException если пользователь не найден
     */
    void deleteUser(Long userId);
}
