package com.example.bankcards.service.card.impl;

import com.example.bankcards.dto.card.CardDto;
import com.example.bankcards.dto.card.CreateCardRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.entity.enums.Role;
import com.example.bankcards.exception.AccessDeniedException;
import com.example.bankcards.exception.AlreadyExistsException;
import com.example.bankcards.exception.InvalidOperationException;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.service.card.CardAdminService;
import com.example.bankcards.service.card.CardUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Реализация сервисов для работы с картами.
 *
 * <p>Реализует оба интерфейса: {@link CardUserService} и {@link CardAdminService}.
 * Методы разделены на секции {@code ADMIN} и {@code USER} для наглядности.</p>
 *
 * <p>Бизнес-правила:</p>
 * <ul>
 *   <li>Карты можно создавать только для пользователей с ролью {@code USER}</li>
 *   <li>Срок действия карты: от текущей даты до +5 лет</li>
 *   <li>Блокировать можно только активные карты</li>
 *   <li>Активировать можно только заблокированные карты</li>
 *   <li>Нельзя заблокировать просроченную карту</li>
 * </ul>
 *
 * @see Card
 * @see CardStatus
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CardServiceImpl implements CardUserService, CardAdminService {
    private final CardRepository cardRepository;

    private final UserRepository userRepository;

    // ========== ADMIN methods (CardAdminService) ==========

    /**
     * Создаёт новую карту от имени администратора.
     *
     * <p>Проверки:</p>
     * <ul>
     *   <li>Пользователь существует и имеет роль {@code USER}</li>
     *   <li>Номер карты уникален</li>
     *   <li>Срок действия не в прошлом и не превышает 5 лет от текущей даты</li>
     * </ul>
     *
     * @param request данные карты
     * @return созданная карта в статусе {@code ACTIVE}
     * @throws NotFoundException если пользователь не найден
     * @throws AccessDeniedException если роль пользователя не USER
     * @throws AlreadyExistsException если номер карты уже занят
     * @throws InvalidOperationException если срок действия некорректен
     */
    @Override
    @Transactional
    public CardDto create(CreateCardRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + request.userId() + " не найден"));

        if (user.getRole() != Role.USER) {
            throw new AccessDeniedException("Карты можно создавать только для обычных пользователей (USER)");
        }

        if (cardRepository.existsByCardNumber(request.cardNumber())) {
            throw new AlreadyExistsException("Карта с таким номером уже существует");
        }

        LocalDate expiryDate = request.getExpiryDateToLocalDate();

        if (expiryDate.isBefore(LocalDate.now())) {
            throw new InvalidOperationException("Срок действия карты не может быть в прошлом");
        }

        if (expiryDate.isAfter(LocalDate.now().plusYears(5))) {
            throw new InvalidOperationException("Срок действия карты не может превышать 5 лет");
        }

        Card card = new Card();
        card.setCardNumber(request.cardNumber());
        card.setExpiryDate(request.expiryDate());
        card.setBalance(request.balance());
        card.setUser(user);
        card.setStatus(CardStatus.ACTIVE);

        return CardDto.fromEntity(cardRepository.save(card));
    }

    /**
     * Возвращает карту по ID.
     *
     * <p>Использует проекцию {@link CardDto} — не загружает связанные сущности.</p>
     *
     * @param cardId идентификатор карты
     * @return DTO карты
     * @throws NotFoundException если карта не найдена
     */
    @Override
    public CardDto findById(Long cardId) {
        return cardRepository.findCardDtoById(cardId).orElseThrow(
                () -> new NotFoundException("Карта с id" + cardId + " не найдена")
        );
    }

    /**
     * Возвращает все карты системы с пагинацией.
     *
     * @param pageable параметры пагинации
     * @return страница с картами
     */
    @Override
    public Page<CardDto> findAll(Pageable pageable) {
        return cardRepository.findAllCardDto(pageable);
    }

    /**
     * Блокирует карту.
     *
     * <p>Можно заблокировать только активную карту.
     * Просроченные и уже заблокированные карты блокировать нельзя.</p>
     *
     * @param cardId идентификатор карты
     * @throws NotFoundException если карта не найдена
     * @throws InvalidOperationException если карта уже заблокирована или просрочена
     */
    @Override
    @Transactional
    public void blockCard(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new NotFoundException("Карта с id " + cardId + " не найдена"));

        if (card.getStatus() == CardStatus.BLOCKED) {
            throw new InvalidOperationException("Карта уже заблокирована");
        }

        if (card.getStatus() == CardStatus.EXPIRED) {
            throw new InvalidOperationException("Нельзя заблокировать просроченную карту");
        }

        card.setStatus(CardStatus.BLOCKED);
    }

    /**
     * Активирует ранее заблокированную карту.
     *
     * <p>Активировать можно только карты в статусе {@code BLOCKED}.</p>
     *
     * @param cardId идентификатор карты
     * @throws NotFoundException если карта не найдена
     * @throws InvalidOperationException если карта не заблокирована
     */
    @Override
    @Transactional
    public void activate(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new NotFoundException("Карта с id " + cardId + " не найдена"));

        if (card.getStatus() != CardStatus.BLOCKED) {
            throw new AccessDeniedException("Активировать можно только заблокированную карту");
        }

        card.setStatus(CardStatus.ACTIVE);
    }

    /**
     * Удаляет карту.
     *
     * <p>Связанные транзакции удаляются каскадно на уровне БД.</p>
     *
     * @param cardId идентификатор карты
     * @throws NotFoundException если карта не найдена
     */
    @Override
    @Transactional
    public void delete(Long cardId) {
        if (!cardRepository.existsById(cardId)) {
            throw new NotFoundException("Карта с id " + cardId + " не найдена");
        }
        cardRepository.deleteById(cardId);
    }

    // ========== USER methods (CardUserService) ==========

    /**
     * Отправляет запрос на блокировку карты пользователем.
     *
     * <p>Карта переводится в статус {@code PENDING_BLOCK}. Администратор
     * позже может подтвердить блокировку через {@link #blockCard(Long)}.</p>
     *
     * <p>Проверки:</p>
     * <ul>
     *   <li>Пользователь существует</li>
     *   <li>Карта принадлежит пользователю</li>
     *   <li>Карта активна</li>
     * </ul>
     *
     * @param cardId идентификатор карты
     * @param userId идентификатор пользователя-владельца
     * @throws NotFoundException если пользователь или карта не найдены
     * @throws AccessDeniedException если карта не принадлежит пользователю
     * @throws InvalidOperationException если карта не в статусе ACTIVE
     */
    @Override
    @Transactional
    public void requestToBlock(Long cardId, Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь с id " + userId + " не найден");
        }

        Card card = cardRepository.findByIdWithUser(cardId).orElseThrow(
                () -> new NotFoundException("Карта с id" + cardId + " не найдена")
        );

        if (!card.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Запрашивать блокировку карты может только владелец карты");
        }

        if (!card.getStatus().equals(CardStatus.ACTIVE)) {
            throw new InvalidOperationException("Блокировать можно только активную карту");
        }

        card.setStatus(CardStatus.PENDING_BLOCK);
    }

    /**
     * Возвращает карты пользователя с возможностью фильтрации по статусу.
     *
     * @param userId   идентификатор пользователя
     * @param status   статус для фильтрации ({@code null} — все карты)
     * @param pageable параметры пагинации
     * @return страница с картами
     * @throws NotFoundException если пользователь не найден
     */
    @Override
    public Page<CardDto> findAllByUserId(Long userId, CardStatus status, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь с id " + userId + " не найден");
        }

        if (status == null) {
            return cardRepository.findAllCardDtoByUserId(userId, pageable);
        }

        return cardRepository.findAllCardDtoByUserIdAndStatus(userId, status, pageable);
    }

    /**
     * Возвращает баланс карты с проверкой принадлежности пользователю.
     *
     * @param cardId идентификатор карты
     * @param userId идентификатор пользователя
     * @return баланс карты
     * @throws NotFoundException если пользователь или карта не найдены
     * @throws AccessDeniedException если карта не принадлежит пользователю
     */
    @Override
    public BigDecimal getBalance(Long cardId, Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь с id " + userId + " не найден");
        }

        CardDto card = findById(cardId);

        if (!card.getOwnerId().equals(userId)) {
            throw new AccessDeniedException("Баланс может смотреть только владелец карты");
        }

        return card.getBalance();
    }
}
