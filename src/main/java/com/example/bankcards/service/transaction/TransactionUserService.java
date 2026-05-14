package com.example.bankcards.service.transaction;

import com.example.bankcards.dto.transaction.TransactionDto;
import com.example.bankcards.dto.transaction.TransferRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Сервис для переводов между картами пользователя.
 *
 * <p>Все операции проверяют, что карты принадлежат текущему пользователю.
 * Перевод возможен только между собственными картами пользователя.</p>
 */
public interface TransactionUserService {
    /**
     * Выполняет перевод средств между двумя картами пользователя.
     *
     * @param request данные перевода (карты и сумма)
     * @param userId  идентификатор пользователя-владельца
     * @return созданная транзакция со статусом {@code SUCCESS}
     * @throws com.example.bankcards.exception.InvalidOperationException если карты совпадают или неактивны
     * @throws com.example.bankcards.exception.NotFoundException если карта не найдена
     * @throws com.example.bankcards.exception.AccessDeniedException если карта не принадлежит пользователю
     * @throws com.example.bankcards.exception.InsufficientFundsException если недостаточно средств
     */
    TransactionDto transferBetweenOwnCards(TransferRequest request, Long userId);

    /**
     * Возвращает историю транзакций по карте.
     *
     * <p>Включает как входящие, так и исходящие переводы.</p>
     *
     * @param cardId   идентификатор карты
     * @param userId   идентификатор пользователя-владельца
     * @param pageable параметры пагинации
     * @return страница с историей транзакций
     * @throws com.example.bankcards.exception.NotFoundException если карта не найдена
     * @throws com.example.bankcards.exception.AccessDeniedException если карта не принадлежит пользователю
     */
    Page<TransactionDto> getTransactionHistory(Long cardId, Long userId, Pageable pageable);
}