package com.example.bankcards.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

/**
 * Расширенная реализация {@link org.springframework.security.core.userdetails.UserDetails} с дополнительным полем {@code userId}.
 *
 * <p>Стандартный {@link User} из Spring Security не содержит ID пользователя,
 * только username, password и authorities. Этот класс добавляет {@code userId},
 * чтобы не обращаться к БД каждый раз для получения ID текущего пользователя.</p>
 *
 * <p>Используется в двух местах:</p>
 * <ul>
 *   <li>{@link JwtRequestFilter} — создаётся из данных токена (stateless)</li>
 *   <li>{@link CustomUserDetailsService} — создаётся при загрузке из БД</li>
 * </ul>
 *
 * <p>Доступ к ID текущего пользователя в контроллерах:</p>
 * <pre>{@code
 * @AuthenticationPrincipal CustomUserDetails userDetails
 * Long userId = userDetails.getUserId();
 * }</pre>
 *
 * @see User
 * @see org.springframework.security.core.userdetails.UserDetails
 */
@Getter
public class CustomUserDetails extends User {

    /**
     * Идентификатор пользователя в БД.
     * Доступен через {@link #getUserId()} (геттер сгенерирован Lombok-аннотацией {@code @Getter}).
     */
    private final Long userId;

    /**
     * Создаёт объект пользователя с дополнительным полем ID.
     *
     * @param userId      идентификатор пользователя в БД
     * @param username    имя пользователя (логин)
     * @param password    пароль (может быть пустым при создании из токена)
     * @param authorities список полномочий (ролей) пользователя
     */
    public CustomUserDetails(
            Long userId,
            String username,
            String password,
            Collection<? extends GrantedAuthority> authorities
    ) {
        super(username, password, authorities);
        this.userId = userId;
    }
}
