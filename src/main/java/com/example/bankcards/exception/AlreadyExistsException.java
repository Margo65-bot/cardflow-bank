package com.example.bankcards.exception;

/**
 * Выбрасывается при попытке создать сущность, которая уже существует.
 *
 * <p>Типичные ситуации:</p>
 * <ul>
 *   <li>Регистрация пользователя с уже занятым username или email</li>
 *   <li>Создание карты с номером, который уже существует в системе</li>
 * </ul>
 *
 * <p>HTTP-статус: 409 Conflict.</p>
 *
 * @see com.example.bankcards.exception.controller.GlobalExceptionHandler#handleAlreadyExists(AlreadyExistsException, jakarta.servlet.http.HttpServletRequest)
 */
public class AlreadyExistsException extends RuntimeException {
    public AlreadyExistsException(String message) {
        super(message);
    }
}