package com.example.bankcards.repository;

import com.example.bankcards.dto.transaction.TransactionDto;
import com.example.bankcards.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Репозиторий для работы с сущностью {@link Transaction}.
 *
 * <p>Все методы возвращают {@link TransactionDto} через JPQL-конструктор,
 * что позволяет избежать загрузки связанных сущностей {@link com.example.bankcards.entity.Card} целиком.</p>
 */
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    /**
     * Возвращает историю транзакций по конкретной карте с пагинацией.
     *
     * <p>В выборку попадают транзакции, где карта является
     * как отправителем ({@code fromCard}), так и получателем ({@code toCard}).
     * Сортировка: от новых к старым.</p>
     *
     * @param cardId   идентификатор карты
     * @param pageable параметры пагинации (сортировка игнорируется — всегда по дате создания)
     * @return страница с историей транзакций карты
     */
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

    /**
     * Возвращает все транзакции в системе с пагинацией (для администратора).
     *
     * <p>Сортировка: от новых к старым. Используется в админ-панели
     * для аудита всех переводов.</p>
     *
     * @param pageable параметры пагинации (сортировка игнорируется — всегда по дате создания)
     * @return страница со всеми транзакциями
     */
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
