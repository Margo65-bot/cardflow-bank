package com.example.bankcards.exception;

/**
 * Выбрасывается при попытке перевода на сумму, превышающую баланс карты.
 *
 * <p>Проверка выполняется в {@code TransactionServiceImpl.transferBetweenOwnCards()}
 * перед списанием средств.</p>
 *
 * <p>HTTP-статус: 400 Bad Request.</p>
 *
 * @see com.example.bankcards.exception.controller.GlobalExceptionHandler#handleInsufficientFunds(InsufficientFundsException, jakarta.servlet.http.HttpServletRequest)
 */
public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(String message) {
        super(message);
    }
}