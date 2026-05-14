package com.example.bankcards.exception.controller;

import com.example.bankcards.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Глобальный обработчик исключений для REST API.
 *
 * <p>Перехватывает все исключения, возникающие в контроллерах,
 * и преобразует их в стандартизированный JSON-ответ {@link ApiError}.</p>
 *
 * <h3>Маппинг исключений на HTTP-статусы:</h3>
 * <pre>
 * MethodArgumentNotValidException    → 400 Bad Request (ошибки валидации)
 * HttpMessageNotReadableException    → 400 Bad Request (некорректный JSON)
 * InsufficientFundsException         → 400 Bad Request (недостаточно средств)
 * InvalidOperationException          → 400 Bad Request (недопустимая операция)
 * BadCredentialsException            → 401 Unauthorized (неверный логин/пароль)
 * AccessDeniedException              → 403 Forbidden (нет прав)
 * NotFoundException                  → 404 Not Found (сущность не найдена)
 * UsernameNotFoundException          → 404 Not Found (пользователь не найден)
 * AlreadyExistsException             → 409 Conflict (сущность уже существует)
 * Exception (запасной)               → 500 Internal Server Error
 * </pre>
 *
 * <p>Все ошибки логируются: 4xx — с уровнем WARN или INFO, 5xx — с уровнем ERROR.</p>
 *
 * @see ApiError
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ========== 400 BAD REQUEST ==========

    /**
     * Обрабатывает ошибки валидации DTO (аннотации {@code @Valid}).
     *
     * <p>Собирает все ошибки валидации в карту: имя поля → сообщение об ошибке.
     * Например, если не заполнен номер карты и некорректный email:</p>
     * <pre>{@code
     * {
     *   "cardNumber": "Номер карты обязателен",
     *   "email": "Введите корректный email адрес"
     * }
     * }</pre>
     *
     * @param ex      исключение с информацией об ошибках валидации
     * @param request HTTP-запрос (для получения URL)
     * @return 400 Bad Request с картой ошибок
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationExceptions(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("Validation error: {}", errors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiError.ofValidationErrors(errors, request.getRequestURI()));
    }

    /**
     * Обрабатывает ошибки парсинга JSON (некорректный формат тела запроса).
     *
     * <p>Срабатывает, когда тело запроса не является валидным JSON
     * или содержит данные неверного типа.</p>
     *
     * @param ex      исключение Spring о невозможности прочитать сообщение
     * @param request HTTP-запрос
     * @return 400 Bad Request с сообщением о некорректном формате
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        log.warn("Malformed JSON request: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of(
                        400,
                        "Bad Request",
                        "Некорректный формат запроса. Проверьте тело запроса.",
                        request.getRequestURI()
                ));
    }

    /**
     * Обрабатывает ошибку недостаточного баланса карты.
     *
     * @param ex      исключение с информацией о недостатке средств
     * @param request HTTP-запрос
     * @return 400 Bad Request с описанием проблемы
     */
    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ApiError> handleInsufficientFunds(
            InsufficientFundsException ex,
            HttpServletRequest request
    ) {
        log.warn("Insufficient funds: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of(
                        400,
                        "Bad Request",
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    /**
     * Обрабатывает ошибки недопустимых бизнес-операций.
     *
     * <p>Например: перевод на ту же карту, блокировка неактивной карты,
     * некорректный срок действия и т.д.</p>
     *
     * @param ex      исключение с описанием недопустимой операции
     * @param request HTTP-запрос
     * @return 400 Bad Request
     */
    @ExceptionHandler(InvalidOperationException.class)
    public ResponseEntity<ApiError> handleInvalidOperation(
            InvalidOperationException ex,
            HttpServletRequest request
    ) {
        log.warn("Invalid operation: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of(
                        400,
                        "Bad Request",
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    // ========== 401 UNAUTHORIZED ==========

    /**
     * Обрабатывает ошибку неверных учётных данных при входе.
     *
     * <p>Выбрасывается Spring Security, когда username или пароль неверны.
     * Сообщение в ответе намеренно не раскрывает, что именно неверно —
     * это защита от перебора.</p>
     *
     * @param ex      исключение Spring Security
     * @param request HTTP-запрос
     * @return 401 Unauthorized
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(
            BadCredentialsException ex,
            HttpServletRequest request
    ) {
        log.warn("Bad credentials for request to {}", request.getRequestURI());

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiError.of(
                        401,
                        "Unauthorized",
                        "Неверное имя пользователя или пароль",
                        request.getRequestURI()
                ));
    }

    // ========== 403 FORBIDDEN ==========

    /**
     * Обрабатывает ошибку доступа к чужому ресурсу.
     *
     * <p>Пользователь аутентифицирован, но не имеет прав на данный ресурс.
     * Например: попытка посмотреть баланс чужой карты.</p>
     *
     * @param ex      исключение с описанием причины отказа
     * @param request HTTP-запрос
     * @return 403 Forbidden
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request
    ) {
        log.warn("Access denied for request to {}: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiError.of(
                        403,
                        "Forbidden",
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    // ========== 404 NOT FOUND ==========

    /**
     * Обрабатывает ошибку отсутствия сущности в БД.
     *
     * <p>Возвращает 404, а не 500 — сущность не найдена,
     * а не сервер упал.</p>
     *
     * @param ex      исключение с описанием (тип сущности и ID)
     * @param request HTTP-запрос
     * @return 404 Not Found
     */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(
            NotFoundException ex,
            HttpServletRequest request
    ) {
        log.info("Not found: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiError.of(
                        404,
                        "Not Found",
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    /**
     * Обрабатывает ошибку поиска пользователя Spring Security.
     *
     * <p>Выбрасывается {@code UserDetailsService}, когда пользователь не найден в БД.
     * Сообщение в ответе общее — без раскрытия username (безопасность).</p>
     *
     * @param ex      исключение Spring Security
     * @param request HTTP-запрос
     * @return 404 Not Found с общим сообщением
     */
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiError> handleUsernameNotFound(
            UsernameNotFoundException ex,
            HttpServletRequest request
    ) {
        log.info("User not found: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiError.of(
                        404,
                        "Not Found",
                        "Пользователь не найден",
                        request.getRequestURI()
                ));
    }

    // ========== 409 CONFLICT ==========

    /**
     * Обрабатывает конфликт при создании дублирующейся сущности.
     *
     * <p>В отличие от 400 (неверный запрос), 409 означает,
     * что запрос синтаксически верен, но конфликтует с текущим состоянием.</p>
     *
     * @param ex      исключение с описанием конфликта
     * @param request HTTP-запрос
     * @return 409 Conflict
     */
    @ExceptionHandler(AlreadyExistsException.class)
    public ResponseEntity<ApiError> handleAlreadyExists(
            AlreadyExistsException ex,
            HttpServletRequest request
    ) {
        log.warn("Conflict: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiError.of(
                        409,
                        "Conflict",
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    // ========== 500 INTERNAL SERVER ERROR ==========

    /**
     * Запасной обработчик для всех необработанных исключений.
     *
     * <p>Любое исключение, не пойманное выше, попадает сюда.
     * Возвращает 500 с общим сообщением. Детали ошибки пишутся в лог
     * с уровнем ERROR для последующего разбора.</p>
     *
     * <p><b>Важно:</b> стектрейс не возвращается клиенту — это требование безопасности.</p>
     *
     * @param ex      непредвиденное исключение
     * @param request HTTP-запрос
     * @return 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of(
                        500,
                        "Internal Server Error",
                        "Внутренняя ошибка сервера. Попробуйте позже.",
                        request.getRequestURI(),
                        List.of(ex.getMessage())
                ));
    }
}