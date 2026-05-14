package com.example.bankcards.service.card;

import com.example.bankcards.dto.card.CardDto;
import com.example.bankcards.dto.card.CreateCardRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Сервис для управления картами от имени администратора.
 *
 * <p>Предоставляет полный CRUD над картами всех пользователей.
 * В отличие от {@link CardUserService}, не проверяет принадлежность карты.</p>
 *
 * @see CardUserService
 */
public interface CardAdminService {
    /**
     * Создаёт новую карту для указанного пользователя.
     *
     * @param request данные карты (номер, срок, владелец, баланс)
     * @return созданная карта в виде {@link CardDto}
     * @throws com.example.bankcards.exception.NotFoundException если пользователь не найден
     * @throws com.example.bankcards.exception.AlreadyExistsException если карта с таким номером уже существует
     * @throws com.example.bankcards.exception.InvalidOperationException если срок действия некорректен
     */
    CardDto create(CreateCardRequest request);

    /**
     * Возвращает все карты системы с пагинацией.
     *
     * @param pageable параметры пагинации и сортировки
     * @return страница с картами
     */
    Page<CardDto> findAll(Pageable pageable);

    /**
     * Находит карту по ID.
     *
     * @param cardId идентификатор карты
     * @return DTO карты
     * @throws com.example.bankcards.exception.NotFoundException если карта не найдена
     */
    CardDto findById(Long cardId);

    /**
     * Блокирует карту.
     *
     * <p>Заблокированная карта не может использоваться для транзакций.</p>
     *
     * @param cardId идентификатор карты
     * @throws com.example.bankcards.exception.NotFoundException если карта не найдена
     * @throws com.example.bankcards.exception.InvalidOperationException если карта уже заблокирована или просрочена
     */
    void blockCard(Long cardId);

    /**
     * Активирует ранее заблокированную карту.
     *
     * @param cardId идентификатор карты
     * @throws com.example.bankcards.exception.NotFoundException если карта не найдена
     * @throws com.example.bankcards.exception.InvalidOperationException если карта не в статусе BLOCKED
     */
    void activate(Long cardId);

    /**
     * Удаляет карту из системы.
     *
     * <p><b>Внимание:</b> операция необратима. Связанные транзакции удаляются каскадно.</p>
     *
     * @param cardId идентификатор карты
     * @throws com.example.bankcards.exception.NotFoundException если карта не найдена
     */
    void delete(Long cardId);
}
