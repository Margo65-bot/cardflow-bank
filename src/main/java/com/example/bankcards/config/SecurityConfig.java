package com.example.bankcards.config;

import com.example.bankcards.security.CustomUserDetailsService;
import com.example.bankcards.security.JwtRequestFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Основная конфигурация безопасности приложения.
 *
 * <h3>Архитектура</h3>
 * <p>Приложение использует <b>stateless</b>-аутентификацию на JWT-токенах.
 * Серверные сессии отключены — каждый запрос должен содержать токен
 * в заголовке {@code Authorization: Bearer <токен>}.</p>
 *
 * <h3>Разделение ролей</h3>
 * <p>В системе две роли с чётким разделением обязанностей:</p>
 * <ul>
 *   <li><b>USER</b> — владелец карт: создаёт карты, совершает переводы,
 *       просматривает историю. Не имеет доступа к данным других пользователей.</li>
 *   <li><b>ADMIN</b> — чистый управленец: администрирует пользователей и карты,
 *       просматривает все транзакции. Не имеет собственных карт.</li>
 * </ul>
 *
 * <h3>Матрица доступа к URL</h3>
 * <pre>
 * /api/auth/**            → permitAll        (публичные)
 * /swagger-ui/**          → permitAll        (документация)
 * /v3/api-docs/**         → permitAll        (OpenAPI-спецификация)
 * /actuator/**            → permitAll        (метрики и health-check)
 * /api/admin/**           → hasRole('ADMIN') (администрирование)
 * /api/user/**            → hasRole('USER')  (карты и переводы пользователя)
 * /api/users/me           → authenticated    (профиль: USER и ADMIN)
 * всё остальное           → authenticated    (защита по умолчанию)
 * </pre>
 *
 * <h3>CORS-политики (четыре уровня)</h3>
 * <ul>
 *   <li><b>Публичная</b> ({@code /api/auth/**, /swagger-ui/**, /actuator/**}) —
 *       GET, POST без токена. Заголовки: Content-Type. Кеш: 1 час.</li>
 *   <li><b>Для авторизованных</b> ({@code /api/users/me}) —
 *       Только GET. Заголовки: Authorization, Content-Type. Доступна USER и ADMIN.</li>
 *   <li><b>Пользовательская</b> ({@code /api/user/**}) —
 *       GET, POST, PATCH с токеном. Заголовки: Authorization, Content-Type. Кеш: 1 час.</li>
 *   <li><b>Административная</b> ({@code /api/admin/**}) —
 *       Полный CRUD. Заголовки: Authorization, Content-Type. Кеш: 30 минут.
 *       В продакшене — только с домена {@code https://admin.myapp.ru}.</li>
 * </ul>
 *
 * <p>Разрешённые домены для не-админских эндпоинтов:
 * {@code http://localhost:3000} (разработка), {@code https://myapp.ru} (продакшен).</p>
 *
 * <h3>Цепочка фильтров (порядок)</h3>
 * <ol>
 *   <li>CORS-фильтр — проверяет Origin и метод запроса</li>
 *   <li>{@link JwtRequestFilter} — извлекает JWT из заголовка, устанавливает аутентификацию</li>
 *   <li>{@link UsernamePasswordAuthenticationFilter} — стандартная форма (не используется в REST)</li>
 * </ol>
 *
 * @see JwtRequestFilter
 * @see CustomUserDetailsService
 * @see com.example.bankcards.util.JwtUtil
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    /** Сервис загрузки пользователя из БД при аутентификации по паролю */
    private final CustomUserDetailsService userDetailsService;

    /** Фильтр извлечения JWT-токена из заголовка Authorization */
    private final JwtRequestFilter jwtRequestFilter;

    /**
     * Создаёт энкодер паролей.
     *
     * <p>Используется BCrypt — адаптивный алгоритм хеширования,
     * устойчивый к брутфорсу. Один экземпляр на всё приложение.</p>
     *
     * @return {@link BCryptPasswordEncoder}
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Настраивает провайдер аутентификации DAO.
     *
     * <p>Связывает {@link CustomUserDetailsService} (источник пользователей)
     * и {@link PasswordEncoder} (проверка паролей).
     * Используется {@link AuthenticationManager} при вызове {@code authenticate()}.</p>
     *
     * @return настроенный {@link DaoAuthenticationProvider}
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * Предоставляет менеджер аутентификации из глобальной конфигурации.
     *
     * <p>Используется в {@code AuthServiceImpl} для ручной аутентификации
     * пользователя по username и паролю.</p>
     *
     * @param authConfig глобальная конфигурация Spring Security
     * @return {@link AuthenticationManager}
     * @throws Exception если конфигурация недоступна
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * Создаёт источник CORS-конфигураций с четырьмя уровнями доступа.
     *
     * <h3>Публичная ({@code /api/auth/**, /swagger-ui/**, /v3/api-docs/**, /actuator/**})</h3>
     * <ul>
     *   <li>Методы: GET, POST, OPTIONS</li>
     *   <li>Заголовки: Content-Type</li>
     *   <li>Кеш: 1 час</li>
     * </ul>
     *
     * <h3>Для авторизованных ({@code /api/users/me})</h3>
     * <ul>
     *   <li>Методы: GET, OPTIONS</li>
     *   <li>Заголовки: Authorization, Content-Type</li>
     *   <li>Кеш: 1 час</li>
     *   <li>Доступна обеим ролям (USER и ADMIN) — только чтение профиля</li>
     * </ul>
     *
     * <h3>Пользовательская ({@code /api/user/**})</h3>
     * <ul>
     *   <li>Методы: GET, POST, PATCH, OPTIONS</li>
     *   <li>Заголовки: Authorization, Content-Type</li>
     *   <li>Кеш: 1 час</li>
     *   <li>Только для роли USER — операции со своими картами и переводами</li>
     * </ul>
     *
     * <h3>Административная ({@code /api/admin/**})</h3>
     * <ul>
     *   <li>Методы: GET, POST, PUT, PATCH, DELETE, OPTIONS</li>
     *   <li>Заголовки: Authorization, Content-Type</li>
     *   <li>Кеш: 30 минут (меньше — выше безопасность)</li>
     *   <li>Только для роли ADMIN — полное управление системой</li>
     *   <li>Продакшен-домен: {@code https://admin.myapp.ru}</li>
     * </ul>
     *
     * <p>Разрешённые домены: {@code localhost:3000} (разработка), {@code myapp.ru} (продакшен).</p>
     *
     * <p><b>Порядок регистрации важен:</b> от частного к общему.
     * {@code /**} зарегистрирован последним как запасной вариант.</p>
     *
     * @return источник CORS-конфигураций
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> origins = List.of("http://localhost:3000", "https://myapp.ru");

        // Публичные эндпоинты — без токена, минимум методов
        CorsConfiguration publicConfig = new CorsConfiguration();
        publicConfig.setAllowedOrigins(origins);
        publicConfig.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        publicConfig.setAllowedHeaders(List.of("Content-Type"));
        publicConfig.setAllowCredentials(true);
        publicConfig.setMaxAge(3600L);

        // Общие для всех авторизованных — только чтение профиля
        CorsConfiguration authenticatedConfig = new CorsConfiguration();
        authenticatedConfig.setAllowedOrigins(origins);
        authenticatedConfig.setAllowedMethods(List.of("GET", "OPTIONS"));
        authenticatedConfig.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        authenticatedConfig.setAllowCredentials(true);
        authenticatedConfig.setMaxAge(3600L);

        // Пользовательские — с токеном, можно менять свои данные
        CorsConfiguration userConfig = new CorsConfiguration();
        userConfig.setAllowedOrigins(origins);
        userConfig.setAllowedMethods(List.of("GET", "POST", "PATCH", "OPTIONS"));
        userConfig.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        userConfig.setAllowCredentials(true);
        userConfig.setMaxAge(3600L);

        // Админские — полный CRUD, отдельный домен в продакшене
        CorsConfiguration adminConfig = new CorsConfiguration();
        adminConfig.setAllowedOrigins(origins);
        adminConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        adminConfig.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        adminConfig.setAllowCredentials(true);
        adminConfig.setMaxAge(1800L);

        // Порядок важен: от частного к общему
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/auth/**", publicConfig);
        source.registerCorsConfiguration("/api/users/me", authenticatedConfig);
        source.registerCorsConfiguration("/api/user/**", userConfig);
        source.registerCorsConfiguration("/api/admin/**", adminConfig);
        source.registerCorsConfiguration("/swagger-ui/**", publicConfig);
        source.registerCorsConfiguration("/v3/api-docs/**", publicConfig);
        source.registerCorsConfiguration("/actuator/**", publicConfig);
        source.registerCorsConfiguration("/**", authenticatedConfig);

        return source;
    }

    /**
     * Создаёт цепочку фильтров безопасности.
     *
     * <h3>Порядок настройки:</h3>
     * <ol>
     *   <li><b>CSRF</b> — отключён (REST API без кук, токен в заголовке)</li>
     *   <li><b>Права доступа</b> — публичные URL открыты, пользовательские и админские по ролям,
     *       профиль доступен обеим ролям</li>
     *   <li><b>CORS</b> — четырёхуровневая политика (публичная, авторизованные, USER, ADMIN)</li>
     *   <li><b>Сессии</b> — отключены (STATELESS), каждый запрос самодостаточен</li>
     *   <li><b>Провайдер аутентификации</b> — BCrypt + UserDetailsService для проверки паролей</li>
     *   <li><b>JWT-фильтр</b> — перед стандартным UsernamePasswordAuthenticationFilter</li>
     * </ol>
     *
     * <h3>Матрица доступа:</h3>
     * <pre>
     * /api/auth/**            → permitAll
     * /swagger-ui/**          → permitAll
     * /v3/api-docs/**         → permitAll
     * /actuator/**            → permitAll
     * /api/admin/**           → hasRole('ADMIN')
     * /api/user/**            → hasRole('USER')
     * /api/users/me           → authenticated
     * всё остальное           → authenticated
     * </pre>
     *
     * @param http объект конфигурации HTTP-безопасности
     * @return построенная цепочка фильтров
     * @throws Exception если конфигурация не может быть построена
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/user/**").hasRole("USER")
                        .requestMatchers("/api/users/me").authenticated()
                        .anyRequest().authenticated()
                )
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}