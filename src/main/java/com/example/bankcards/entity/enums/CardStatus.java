package com.example.bankcards.entity.enums;

/**
 * Статус банковской карты.
 *
 * <p>Жизненный цикл карты:</p>
 * <ol>
 *   <li>{@code ACTIVE} — карта активна, можно совершать операции</li>
 *   <li>{@code PENDING_BLOCK} — запрошена блокировка пользователем, ожидает подтверждения администратора</li>
 *   <li>{@code BLOCKED} — карта заблокирована администратором, операции запрещены</li>
 *   <li>{@code EXPIRED} — срок действия карты истёк (выставляется автоматически планировщиком)</li>
 * </ol>
 * @see com.example.bankcards.scheduler.CardExpiryScheduler
 */
public enum CardStatus {
    ACTIVE,
    BLOCKED,
    EXPIRED,
    PENDING_BLOCK
}
