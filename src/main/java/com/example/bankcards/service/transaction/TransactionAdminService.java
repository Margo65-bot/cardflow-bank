package com.example.bankcards.service.transaction;

import com.example.bankcards.dto.transaction.TransactionDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Сервис для просмотра транзакций от имени администратора.
 *
 * <p>Даёт доступ к просмотру транзакций системы без ограничений.</p>
 */
public interface TransactionAdminService {
    /**
     * Возвращает все транзакции в системе с пагинацией.
     *
     * @param pageable параметры пагинации
     * @return страница с транзакциями, отсортированными от новых к старым
     */
    Page<TransactionDto> getAllTransactions(Pageable pageable);
}