package com.example.bankcards.exception;

/**
 * Выбрасывается при попытке доступа к ресурсу без необходимых прав.
 *
 * <p>Типичные ситуации:</p>
 * <ul>
 *   <li>Пользователь пытается получить доступ к карте, которая ему не принадлежит</li>
 *   <li>Пользователь без роли ADMIN пытается выполнить административную операцию</li>
 *   <li>Попытка назначить роль ADMIN пользователю, у которого есть карты</li>
 * </ul>
 *
 * <p>HTTP-статус: 403 Forbidden.</p>
 *
 * @see com.example.bankcards.exception.controller.GlobalExceptionHandler#handleAccessDenied(AccessDeniedException, jakarta.servlet.http.HttpServletRequest)
 */
public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException(String message) {
        super(message);
    }
}