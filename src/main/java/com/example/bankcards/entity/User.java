package com.example.bankcards.entity;

import com.example.bankcards.entity.enums.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Сущность пользователя системы.
 *
 * <p>Хранит учётные данные и роль пользователя.
 * Пароль автоматически хешируется через {@link BCryptPasswordEncoder} при установке
 * и никогда не хранится в открытом виде.</p>
 *
 * <p>Связи:</p>
 * <ul>
 *   <li>Один пользователь может иметь много карт (One-to-Many к {@link Card})</li>
 * </ul>
 *
 * @see Role
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@ToString
public class User {
    /** Энкодер паролей — один экземпляр на весь класс (статический) */
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    /** Уникальный идентификатор пользователя, генерируется автоматически */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Уникальное имя пользователя (логин), от 1 до 50 символов */
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /** Захэшированный пароль. Исключён из {@code toString()} для безопасности */
    @Column(nullable = false)
    @ToString.Exclude
    private String password;

    /** Уникальный email пользователя, до 100 символов */
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    /** Роль пользователя: {@code USER} или {@code ADMIN} */
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Role role;

    /**
     * Устанавливает пароль с автоматическим хешированием.
     *
     * <p>Метод переопределён — на вход принимает сырой пароль,
     * сохраняет захэшированный через {@link BCryptPasswordEncoder}.</p>
     *
     * @param rawPassword сырой пароль, введённый пользователем
     */
    public void setPassword(String rawPassword) {
        this.password = PASSWORD_ENCODER.encode(rawPassword);
    }

    /**
     * Проверяет, соответствует ли сырой пароль захэшированному.
     *
     * @param rawPassword сырой пароль для проверки
     * @return {@code true} если пароль верный, иначе {@code false}
     */
    public boolean checkPassword(String rawPassword) {
        return PASSWORD_ENCODER.matches(rawPassword, this.password);
    }
}
