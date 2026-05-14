package com.example.bankcards.dto.user;

import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.Role;
import lombok.Builder;
import lombok.Getter;

/**
 * DTO для передачи данных пользователя клиенту.
 *
 * <p><b>Важно:</b> пароль никогда не передаётся в DTO.</p>
 */
@Getter
@Builder
public class UserDto {
    /** Уникальный идентификатор пользователя */
    private Long id;

    /** Имя пользователя (логин) */
    private String username;

    /** Email пользователя */
    private String email;

    /** Роль пользователя (USER или ADMIN) */
    private Role role;

    /**
     * Создаёт DTO из сущности {@link User}.
     *
     * <p>Пароль в DTO не копируется.</p>
     *
     * @param user сущность пользователя из БД
     * @return готовый DTO или {@code null}, если {@code user == null}
     */
    public static UserDto fromEntity(User user) {
        if (user == null) return null;
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}
