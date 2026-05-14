package com.example.bankcards.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Стандартизированный DTO для ответов с ошибками.
 *
 * <p>Используется {@link com.example.bankcards.exception.controller.GlobalExceptionHandler} для формирования
 * единообразных JSON-ответов при возникновении исключений.</p>
 *
 * <h3>Структура ответа:</h3>
 * <pre>{@code
 * {
 *   "status": 404,
 *   "error": "Not Found",
 *   "message": "Карта с id 42 не найдена",
 *   "timestamp": "2026-05-14 15:30:00",
 *   "path": "/api/user/cards/42"
 * }
 * }</pre>
 *
 * <h3>Для ошибок валидации добавляется поле validationErrors:</h3>
 * <pre>{@code
 * {
 *   "status": 400,
 *   "error": "Bad Request",
 *   "message": "Ошибка валидации",
 *   "validationErrors": {
 *     "cardNumber": "Номер карты должен содержать 16 цифр",
 *     "expiryDate": "Срок действия должен быть в формате ММ/ГГ"
 *   }
 * }
 * }</pre>
 *
 * <p>Поля {@code validationErrors} и {@code errors} исключаются из JSON,
 * если они {@code null} (благодаря {@link JsonInclude.Include#NON_NULL}).</p>
 *
 * @param status           HTTP-статус ответа
 * @param error            краткое название ошибки (Bad Request, Not Found, ...)
 * @param message          человекочитаемое описание ошибки
 * @param timestamp        время возникновения ошибки
 * @param path             URL запроса, вызвавшего ошибку
 * @param validationErrors карта ошибок валидации (поле → сообщение), может быть {@code null}
 * @param errors           список дополнительных сообщений, может быть {@code null}
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public record ApiError(
        int status,
        String error,
        String message,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime timestamp,
        String path,
        Map<String, String> validationErrors,
        List<String> errors
) {

    /**
     * Создаёт ответ для ошибок валидации (400 Bad Request).
     *
     * <p>Используется при срабатывании {@code @Valid} на DTO,
     * когда не прошли проверки аннотаций ({@code @NotBlank}, {@code @Pattern} и др.).</p>
     *
     * @param validationErrors карта ошибок: имя поля → текст ошибки
     * @param path             URL запроса
     * @return объект {@link ApiError} со статусом 400
     */
    public static ApiError ofValidationErrors(
            Map<String, String> validationErrors,
            String path) {
        return ApiError.builder()
                .status(400)
                .error("Bad Request")
                .message("Ошибка валидации")
                .timestamp(LocalDateTime.now())
                .path(path)
                .validationErrors(validationErrors)
                .build();
    }

    /**
     * Создаёт ответ для простой ошибки без дополнительных деталей.
     *
     * <p>Используется для большинства бизнес-исключений:
     * {@link NotFoundException}, {@link AccessDeniedException},
     * {@link AlreadyExistsException} и др.</p>
     *
     * @param status  HTTP-статус
     * @param error   краткое название ошибки
     * @param message описание ошибки
     * @param path    URL запроса
     * @return объект {@link ApiError}
     */
    public static ApiError of(int status, String error, String message, String path) {
        return ApiError.builder()
                .status(status)
                .error(error)
                .message(message)
                .timestamp(LocalDateTime.now())
                .path(path)
                .build();
    }

    /**
     * Создаёт ответ с дополнительными деталями (списком сообщений).
     *
     * <p>Используется для ошибок, где нужно передать несколько сообщений,
     * например, при внутренней ошибке сервера для логирования.</p>
     *
     * @param status  HTTP-статус
     * @param error   краткое название ошибки
     * @param message основное описание
     * @param path    URL запроса
     * @param errors  список дополнительных сообщений
     * @return объект {@link ApiError}
     */
    public static ApiError of(int status, String error, String message, String path, List<String> errors) {
        return ApiError.builder()
                .status(status)
                .error(error)
                .message(message)
                .timestamp(LocalDateTime.now())
                .path(path)
                .errors(errors)
                .build();
    }
}