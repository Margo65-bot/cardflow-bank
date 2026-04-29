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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CardServiceImpl implements CardUserService, CardAdminService {
    private final CardRepository cardRepository;

    private final UserRepository userRepository;

    // ========== ADMIN methods (CardAdminService) ==========

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

        Card card = new Card();
        card.setCardNumber(request.cardNumber());
        card.setExpiryDate(request.expiryDate());
        card.setBalance(request.balance());
        card.setUser(user);
        card.setStatus(CardStatus.ACTIVE);

        return CardDto.fromEntity(cardRepository.save(card));
    }

    @Override
    public CardDto findById(Long cardId) {
        return cardRepository.findCardDtoById(cardId).orElseThrow(
                () -> new NotFoundException("Карта с id" + cardId + " не найдена")
        );
    }

    @Override
    public Page<CardDto> findAll(Pageable pageable) {
        return cardRepository.findAllCardDto(pageable);
    }

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

    @Override
    @Transactional
    public void delete(Long cardId) {
        if (!cardRepository.existsById(cardId)) {
            throw new NotFoundException("Карта с id " + cardId + " не найдена");
        }
        cardRepository.deleteById(cardId);
    }

    // ========== USER methods (CardUserService) ==========

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
