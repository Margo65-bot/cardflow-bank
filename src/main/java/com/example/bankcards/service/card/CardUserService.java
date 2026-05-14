package com.example.bankcards.service.card;

import com.example.bankcards.dto.card.CardDto;
import com.example.bankcards.entity.enums.CardStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

/**
 * Сервис для операций пользователя со своими картами.
 *
 * <p>Все методы проверяют, что карта принадлежит текущему пользователю.
 * В отличие от {@link CardAdminService}, не даёт полного доступа ко всем картам.</p>
 *
 * @see CardAdminService
 */
public interface CardUserService {
    /**
     * Возвращает карты пользователя с возможностью фильтрации по статусу.
     *
     * @param userId   идентификатор пользователя-владельца
     * @param status   статус для фильтрации (опционально, {@code null} — без фильтра)
     * @param pageable параметры пагинации
     * @return страница с картами пользователя
     * @throws com.example.bankcards.exception.NotFoundException если пользователь не найден
     */
    Page<CardDto> findAllByUserId(Long userId, CardStatus status, Pageable pageable);

    /**
     * Отправляет запрос на блокировку карты.
     *
     * <p>Карта не блокируется немедленно — только меняет статус на {@code PENDING_BLOCK}.
     * Окончательное решение принимает администратор.</p>
     *
     * @param cardId идентификатор карты
     * @param userId идентификатор пользователя (должен быть владельцем)
     * @throws com.example.bankcards.exception.NotFoundException если пользователь или карта не найдены
     * @throws com.example.bankcards.exception.AccessDeniedException если пользователь не является владельцем
     * @throws com.example.bankcards.exception.InvalidOperationException если карта не в статусе ACTIVE
     */
    void requestToBlock(Long cardId, Long userId);

    /**
     * Возвращает баланс конкретной карты.
     *
     * @param cardId идентификатор карты
     * @param userId идентификатор пользователя (должен быть владельцем)
     * @return текущий баланс карты
     * @throws com.example.bankcards.exception.NotFoundException если пользователь или карта не найдены
     * @throws com.example.bankcards.exception.AccessDeniedException если пользователь не является владельцем
     */
    BigDecimal getBalance(Long cardId, Long userId);
}
