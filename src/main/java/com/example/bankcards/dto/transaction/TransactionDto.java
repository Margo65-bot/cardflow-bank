package com.example.bankcards.dto.transaction;

import com.example.bankcards.entity.Transaction;
import com.example.bankcards.entity.enums.TransactionStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO для передачи данных транзакции клиенту.
 *
 * <p>Содержит информацию о переводе между двумя картами.</p>
 */
@Getter
@Builder
public class TransactionDto {
    /** Уникальный идентификатор транзакции */
    private Long id;

    /** ID карты, с которой списали средства */
    private Long fromCardId;

    /** ID карты, на которую зачислили средства */
    private Long toCardId;

    /** Сумма перевода */
    private BigDecimal amount;

    /** Статус транзакции (PENDING, SUCCESS, FAILED) */
    private TransactionStatus status;

    /** Дата и время создания транзакции */
    private LocalDateTime createdAt;

    /**
     * Создаёт DTO из сущности {@link Transaction}.
     *
     * @param transaction сущность транзакции из БД
     * @return готовый DTO или {@code null}, если {@code transaction == null}
     */
    public static TransactionDto fromEntity(Transaction transaction) {
        if (transaction == null) return null;
        return TransactionDto.builder()
                .id(transaction.getId())
                .fromCardId(transaction.getFromCard().getId())
                .toCardId(transaction.getToCard().getId())
                .amount(transaction.getAmount())
                .status(transaction.getStatus())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
