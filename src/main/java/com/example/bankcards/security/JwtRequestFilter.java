package com.example.bankcards.security;

import com.example.bankcards.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * Фильтр для извлечения JWT-токена из заголовка {@code Authorization} и установки аутентификации.
 *
 * <p>Выполняется <b>один раз на каждый запрос</b> (наследует {@link OncePerRequestFilter}).
 * Стоит в цепочке фильтров <b>перед</b> {@link org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter}.</p>
 *
 * <p>Алгоритм работы:</p>
 * <ol>
 *   <li>Извлекает заголовок {@code Authorization}</li>
 *   <li>Проверяет, что он начинается с {@code Bearer }</li>
 *   <li>Извлекает токен (всё после {@code Bearer })</li>
 *   <li>Извлекает username и проверяет валидность токена</li>
 *   <li>Если токен валиден — создаёт {@link UsernamePasswordAuthenticationToken}
 *       и помещает его в {@link SecurityContextHolder}</li>
 *   <li>Пропускает запрос дальше по цепочке фильтров</li>
 * </ol>
 *
 * <p><b>Важно:</b> фильтр не обращается к БД для проверки существования пользователя.
 * Вся необходимая информация (userId, username, role) извлекается из токена.
 * Это сделано для производительности — при stateless-архитектуре нет смысла
 * проверять пользователя в БД на каждый запрос.</p>
 *
 * @see JwtUtil
 * @see CustomUserDetails
 */
@Component
@RequiredArgsConstructor
public class JwtRequestFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    /**
     * Сервис для загрузки пользователя из БД.
     *
     * <p><b>Примечание:</b> в текущей реализации не используется —
     * данные пользователя извлекаются напрямую из токена.
     * Оставлен для возможного расширения (например, проверка, не удалён ли пользователь).</p>
     */
    private final CustomUserDetailsService userDetailsService;

    /**
     * Основной метод фильтрации — вызывается при каждом HTTP-запросе.
     *
     * <p>Если токен отсутствует или невалиден — аутентификация не устанавливается,
     * и запрос проходит дальше без неё. В этом случае сработает
     * стандартная защита Spring Security (вернёт 401 или 403).</p>
     *
     * @param request     HTTP-запрос
     * @param response    HTTP-ответ
     * @param filterChain цепочка фильтров
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        final String authorizationHeader = request.getHeader("Authorization");

        String username = null;
        String jwt = null;

        // Извлекаем токен из заголовка Authorization: Bearer <token>
        if (authorizationHeader != null
                && !authorizationHeader.isBlank()
                && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);  // отрезаем "Bearer "
            username = jwtUtil.extractUsername(jwt);
        }

        // Если пользователь ещё не аутентифицирован и токен валиден — устанавливаем аутентификацию
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            if (jwtUtil.isTokenValid(jwt)) {
                Long userId = jwtUtil.extractUserId(jwt);
                String role = jwtUtil.extractRole(jwt);

                // Создаём объект пользователя без обращения к БД (stateless)
                UsernamePasswordAuthenticationToken authToken = getUsernamePasswordAuthenticationToken(userId, username, role);
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        // Пропускаем запрос дальше по цепочке
        filterChain.doFilter(request, response);
    }

    private static UsernamePasswordAuthenticationToken getUsernamePasswordAuthenticationToken(Long userId, String username, String role) {
        CustomUserDetails userDetails = new CustomUserDetails(
                userId,
                username,
                "",  // пароль не нужен — пользователь уже аутентифицирован токеном
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
        );

        // Создаём токен аутентификации и помещаем в контекст безопасности
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities()
                );
        return authToken;
    }
}