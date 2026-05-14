package com.example.bankcards.service.user;

import com.example.bankcards.dto.security.RegisterRequest;
import com.example.bankcards.dto.user.UserDto;

/**
 * Сервис для операций с пользователями.
 *
 * <p>Содержит методы для публичной регистрации и получения профиля.
 * Пароли автоматически хешируются через {@code BCrypt} при сохранении.</p>
 */
public interface UserService {
    /**
     * Регистрирует нового пользователя с ролью {@code USER}.
     *
     * <p>Проверяет уникальность username и email.
     * Пароль автоматически хешируется.</p>
     *
     * @param request данные для регистрации
     * @return созданный пользователь
     * @throws com.example.bankcards.exception.AlreadyExistsException если username или email заняты
     */
    UserDto register(RegisterRequest request);

    /**
     * Находит пользователя по ID.
     *
     * @param userId идентификатор пользователя
     * @return DTO пользователя (без пароля)
     * @throws com.example.bankcards.exception.NotFoundException если пользователь не найден
     */
    UserDto findById(Long userId);

    /**
     * Находит пользователя по username.
     *
     * <p>Используется внутри системы для аутентификации.
     * Не должен быть доступен через публичное API.</p>
     *
     * @param username имя пользователя
     * @return DTO пользователя (без пароля)
     * @throws com.example.bankcards.exception.NotFoundException если пользователь не найден
     */
    UserDto findByUsername(String username);
}