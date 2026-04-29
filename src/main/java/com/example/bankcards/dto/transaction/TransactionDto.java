package com.example.bankcards.dto.transaction;

import com.example.bankcards.entity.Transaction;
import com.example.bankcards.entity.enums.TransactionStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class TransactionDto {
    private Long id;
    private Long fromCardId;
    private Long toCardId;
    private BigDecimal amount;
    private TransactionStatus status;
    private LocalDateTime createdAt;

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
