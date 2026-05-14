package com.example.bankcards.repository;

import com.example.bankcards.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Репозиторий для работы с сущностью {@link User}.
 *
 * <p>Содержит методы для поиска пользователей по username и проверки
 * уникальности username и email.</p>
 */
public interface UserRepository extends JpaRepository<User, Long> {
    /**
     * Находит пользователя по имени (логину).
     *
     * <p>Используется при аутентификации — метод {@code loadUserByUsername}
     * в {@code CustomUserDetailsService}.</p>
     *
     * @param username имя пользователя (логин)
     * @return {@link Optional} с пользователем или {@code Optional.empty()} если не найден
     */
    Optional<User> findByUsername(String username);

    /**
     * Проверяет, занято ли имя пользователя.
     *
     * <p>Используется при регистрации для проверки уникальности username.</p>
     *
     * @param username имя пользователя для проверки
     * @return {@code true} если пользователь с таким именем уже существует
     */
    boolean existsByUsername(String username);

    /**
     * Проверяет, занят ли email.
     *
     * <p>Используется при регистрации для проверки уникальности email.</p>
     *
     * @param email email для проверки
     * @return {@code true} если пользователь с таким email уже существует
     */
    boolean existsByEmail(String email);
}
