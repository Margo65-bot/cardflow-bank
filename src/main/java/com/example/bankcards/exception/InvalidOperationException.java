package com.example.bankcards.exception;

/**
 * Выбрасывается при попытке выполнить операцию, которая нарушает бизнес-правила.
 *
 * <p>Типичные ситуации:</p>
 * <ul>
 *   <li>Попытка перевода на ту же карту</li>
 *   <li>Блокировка уже заблокированной или просроченной карты</li>
 *   <li>Активация карты, которая не находится в статусе BLOCKED</li>
 *   <li>Операция с неактивной картой</li>
 *   <li>Некорректный срок действия карты</li>
 * </ul>
 *
 * <p>HTTP-статус: 400 Bad Request.</p>
 *
 * @see com.example.bankcards.exception.controller.GlobalExceptionHandler#handleInvalidOperation(InvalidOperationException, jakarta.servlet.http.HttpServletRequest)
 */
public class InvalidOperationException extends RuntimeException {
    public InvalidOperationException(String message) {
        super(message);
    }
}