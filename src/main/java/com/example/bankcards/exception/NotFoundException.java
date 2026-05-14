package com.example.bankcards.exception;

/**
 * Выбрасывается, когда запрашиваемая сущность не найдена в БД.
 *
 * <p>Типичные ситуации:</p>
 * <ul>
 *   <li>Карта с указанным ID не существует</li>
 *   <li>Пользователь с указанным ID или username не найден</li>
 *   <li>Транзакция не найдена</li>
 * </ul>
 *
 * <p>HTTP-статус: 404 Not Found.</p>
 *
 * @see com.example.bankcards.exception.controller.GlobalExceptionHandler#handleNotFound(NotFoundException, jakarta.servlet.http.HttpServletRequest)
 */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}