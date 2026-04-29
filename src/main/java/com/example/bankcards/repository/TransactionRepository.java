package com.example.bankcards.repository;

import com.example.bankcards.dto.transaction.TransactionDto;
import com.example.bankcards.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    @Query("""
        SELECT new com.example.bankcards.dto.transaction.TransactionDto(
            t.id,
            t.fromCard.id,
            t.toCard.id,
            t.amount,
            t.status,
            t.createdAt
        )
        FROM Transaction t
        WHERE t.fromCard.id = :cardId OR t.toCard.id = :cardId
        ORDER BY t.createdAt DESC
    """)
    Page<TransactionDto> findTransactionHistoryByCardId(@Param("cardId") Long cardId, Pageable pageable);

    @Query("""
        SELECT new com.example.bankcards.dto.transaction.TransactionDto(
            t.id,
            t.fromCard.id,
            t.toCard.id,
            t.amount,
            t.status,
            t.createdAt
        )
        FROM Transaction t
        ORDER BY t.createdAt DESC
    """)
    Page<TransactionDto> findAllTransactionDto(Pageable pageable);
}
