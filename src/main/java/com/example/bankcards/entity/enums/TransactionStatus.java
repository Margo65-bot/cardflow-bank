package com.example.bankcards.entity.enums;

/**
 * Статус транзакции (перевода).
 *
 * <p>Жизненный цикл:</p>
 * <ol>
 *   <li>{@code PENDING} — транзакция создана, но ещё не обработана</li>
 *   <li>{@code SUCCESS} — средства успешно переведены, балансы обновлены</li>
 *   <li>{@code FAILED} — перевод не удался (недостаточно средств, карта заблокирована и т.д.)</li>
 * </ol>
 */
public enum TransactionStatus {
    PENDING,
    SUCCESS,
    FAILED
}