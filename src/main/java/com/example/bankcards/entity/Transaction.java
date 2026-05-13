package com.example.bankcards.entity;

import com.example.bankcards.entity.enums.TransactionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Сущность транзакции (перевода) между двумя картами.
 *
 * <p>Фиксирует факт перевода средств с одной карты на другую.
 * Обе карты должны существовать в системе.</p>
 *
 * <p>Связи:</p>
 * <ul>
 *   <li>Many-to-One к {@link Card} — карта-отправитель ({@code fromCard})</li>
 *   <li>Many-to-One к {@link Card} — карта-получатель ({@code toCard})</li>
 * </ul>
 *
 * @see TransactionStatus
 */
@Entity
@Table(name = "transactions")
@Getter
@Setter
@ToString
public class Transaction {
    /** Уникальный идентификатор транзакции, генерируется автоматически */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Карта, с которой списываются средства. Загружается лениво */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_card_id", nullable = false)
    @ToString.Exclude
    private Card fromCard;

    /** Карта, на которую зачисляются средства. Загружается лениво */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_card_id", nullable = false)
    @ToString.Exclude
    private Card toCard;

    /** Сумма перевода в валюте системы. Не может быть отрицательной */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    /**
     * Статус транзакции.
     * Возможные значения: {@code PENDING}, {@code SUCCESS}, {@code FAILED}
     */
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    /** Дата и время создания транзакции */
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
