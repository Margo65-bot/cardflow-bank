package com.example.bankcards.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Утилитный класс для работы с JWT-токенами.
 *
 * <p>Отвечает за:</p>
 * <ul>
 *   <li>Генерацию токенов с использованием алгоритма HMAC-SHA256</li>
 *   <li>Извлечение данных из токена (username, userId, role)</li>
 *   <li>Валидацию токенов (проверка подписи и срока жизни)</li>
 * </ul>
 *
 * <p>Структура payload токена:</p>
 * <ul>
 *   <li>{@code sub} — имя пользователя (subject)</li>
 *   <li>{@code userId} — идентификатор пользователя в БД</li>
 *   <li>{@code role} — роль пользователя (USER или ADMIN)</li>
 *   <li>{@code iat} — время создания (issued at)</li>
 *   <li>{@code exp} — время истечения (expiration)</li>
 * </ul>
 *
 * <p><b>Настройка через application.yml:</b></p>
 * <ul>
 *   <li>{@code jwt.secret} — секретный ключ для подписи (минимум 256 бит / 32 символа)</li>
 *   <li>{@code jwt.expiration} — срок жизни токена в миллисекундах</li>
 * </ul>
 *
 * <p><b>Важно:</b> при смене секретного ключа все ранее выданные токены станут невалидными.</p>
 *
 * @see io.jsonwebtoken.Jwts
 * @see io.jsonwebtoken.security.Keys
 */
@Component
public class JwtUtil {
    /** Секретный ключ для подписи токена, внедряется из {@code jwt.secret} */
    @Value("${jwt.secret}")
    private String secret;

    /** Срок жизни токена в миллисекундах, внедряется из {@code jwt.expiration} */
    @Value("${jwt.expiration}")
    private Long expiration;

    /**
     * Генерирует JWT-токен для пользователя.
     *
     * <p>В токен записываются:</p>
     * <ul>
     *   <li>{@code userId} — идентификатор пользователя</li>
     *   <li>{@code sub} — имя пользователя</li>
     *   <li>{@code role} — роль (USER / ADMIN)</li>
     *   <li>{@code iat} — текущее время</li>
     *   <li>{@code exp} — текущее время + срок жизни из конфигурации</li>
     * </ul>
     *
     * <p>Подпись: HMAC-SHA256 с секретным ключом из конфигурации.</p>
     *
     * @param userId   идентификатор пользователя в БД
     * @param username имя пользователя (логин)
     * @param role     роль пользователя ({@code USER} или {@code ADMIN})
     * @return строка с JWT-токеном в формате {@code header.payload.signature}
     */
    public String generateToken(Long userId, String username, String role) {
        return Jwts.builder()
                .claim("userId", userId)
                .subject(username)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Извлекает имя пользователя (subject) из токена.
     *
     * @param token JWT-токен
     * @return имя пользователя
     * @throws io.jsonwebtoken.JwtException если токен просрочен, невалиден или подпись неверна
     */
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Извлекает идентификатор пользователя из токена.
     *
     * @param token JWT-токен
     * @return ID пользователя в БД
     * @throws io.jsonwebtoken.JwtException если токен невалиден
     */
    public Long extractUserId(String token) {
        return extractAllClaims(token).get("userId", Long.class);
    }

    /**
     * Извлекает роль пользователя из токена.
     *
     * @param token JWT-токен
     * @return роль в виде строки ({@code USER} или {@code ADMIN})
     * @throws io.jsonwebtoken.JwtException если токен невалиден
     */
    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    /**
     * Проверяет валидность JWT-токена.
     *
     * <p>Проверяет:</p>
     * <ul>
     *   <li>Подпись токена (не подделан ли)</li>
     *   <li>Срок жизни (не просрочен ли)</li>
     *   <li>Структуру токена (корректный ли формат)</li>
     * </ul>
     *
     * <p><b>Не проверяет</b> существование пользователя в БД —
     * только криптографическую валидность токена.</p>
     *
     * @param token JWT-токен
     * @return {@code true} если токен валиден, {@code false} в противном случае
     */
    public Boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Создаёт ключ подписи из строкового секрета.
     *
     * <p>Использует алгоритм HMAC-SHA256. Секретный ключ преобразуется
     * в байты в кодировке UTF-8.</p>
     *
     * @return ключ для подписи и верификации токенов
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Извлекает все claims из токена.
     *
     * <p>Выполняет парсинг и верификацию токена в одном вызове.
     * Если токен невалиден — выбрасывает {@link io.jsonwebtoken.JwtException}.</p>
     *
     * @param token JWT-токен
     * @return объект {@link Claims} со всеми данными токена
     * @throws io.jsonwebtoken.JwtException если токен просрочен, невалиден или подпись неверна
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}