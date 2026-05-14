package com.example.bankcards.security;

import com.example.bankcards.entity.User;
import com.example.bankcards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Реализация {@link UserDetailsService} для загрузки пользователя из БД.
 *
 * <p>Используется Spring Security при аутентификации через
 * {@link org.springframework.security.authentication.AuthenticationManager} (логин по username и паролю).</p>
 *
 * <p>Загружает пользователя по username, преобразует в {@link CustomUserDetails}
 * с добавлением ID и роли. Роль оборачивается в {@code ROLE_} префикс,
 * как того требует Spring Security.</p>
 *
 * <p><b>Где используется:</b></p>
 * <ul>
 *   <li>{@link org.springframework.security.authentication.dao.DaoAuthenticationProvider} — при проверке пароля</li>
 *   <li>Не используется в {@link JwtRequestFilter} — там данные берутся из токена</li>
 * </ul>
 *
 * @see UserRepository
 * @see CustomUserDetails
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Загружает пользователя по имени (логину).
     *
     * <p>Вызывается Spring Security автоматически при попытке аутентификации.
     * Преобразует роль из {@code enum Role} в строку с префиксом {@code ROLE_}
     * (например, {@code USER → ROLE_USER}).</p>
     *
     * @param username имя пользователя для поиска
     * @return объект {@link CustomUserDetails} с ID, username, паролем и ролью
     * @throws UsernameNotFoundException если пользователь с таким именем не найден
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден: " + username));

        return new CustomUserDetails(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
                )
        );
    }
}
