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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceImplTest {
    @Mock
    private CardRepository cardRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CardServiceImpl cardService;

    private User testUser;
    private Card testCard;
    private CardDto testCardDto;

    private final Long userId = 1L;
    private final Long cardId = 1L;
    private final String cardNumber = "1234567890123456";
    private final String expiryDate = "12/28";
    private final BigDecimal balance = new BigDecimal("500.00");

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(userId);
        testUser.setUsername("testuser");
        testUser.setRole(Role.USER);

        testCard = new Card();
        testCard.setId(cardId);
        testCard.setCardNumber(cardNumber);
        testCard.setExpiryDate(expiryDate);
        testCard.setStatus(CardStatus.ACTIVE);
        testCard.setBalance(balance);
        testCard.setUser(testUser);

        testCardDto = CardDto.builder()
                .id(cardId)
                .maskedNumber("**** **** **** 3456")
                .expiryDate(expiryDate)
                .status(CardStatus.ACTIVE)
                .balance(balance)
                .ownerId(userId)
                .build();
    }

    // ==================== CREATE (ADMIN) ====================

    @Test
    void create_ShouldCreateCard_WhenValid() {
        CreateCardRequest request = new CreateCardRequest(
                cardNumber, expiryDate, userId, balance
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(cardRepository.existsByCardNumber(cardNumber)).thenReturn(false);
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        CardDto result = cardService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.getBalance()).isEqualTo(balance);
        verify(cardRepository, times(1)).save(any(Card.class));
    }

    @Test
    void create_ShouldThrowNotFoundException_WhenUserNotFound() {
        CreateCardRequest request = new CreateCardRequest(
                cardNumber, expiryDate, 999L, balance
        );

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.create(request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("не найден");
    }

    @Test
    void create_ShouldThrowAccessDeniedException_WhenUserIsNotUSER() {
        testUser.setRole(Role.ADMIN);
        CreateCardRequest request = new CreateCardRequest(
                cardNumber, expiryDate, userId, balance
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> cardService.create(request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("только для обычных пользователей");
    }

    @Test
    void create_ShouldThrowAlreadyExistsException_WhenCardNumberExists() {
        CreateCardRequest request = new CreateCardRequest(
                cardNumber, expiryDate, userId, balance
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(cardRepository.existsByCardNumber(cardNumber)).thenReturn(true);

        assertThatThrownBy(() -> cardService.create(request))
                .isInstanceOf(AlreadyExistsException.class)
                .hasMessageContaining("уже существует");
    }

    // ==================== FIND BY ID ====================

    @Test
    void findById_ShouldReturnCardDto_WhenCardExists() {
        when(cardRepository.findCardDtoById(cardId)).thenReturn(Optional.of(testCardDto));

        CardDto result = cardService.findById(cardId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(cardId);
        verify(cardRepository, times(1)).findCardDtoById(cardId);
    }

    @Test
    void findById_ShouldThrowNotFoundException_WhenCardNotFound() {
        when(cardRepository.findCardDtoById(cardId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.findById(cardId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("не найдена");
    }

    // ==================== FIND ALL ====================

    @Test
    void findAll_ShouldReturnPageOfCards() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<CardDto> cardPage = new PageImpl<>(List.of(testCardDto), pageable, 1);

        when(cardRepository.findAllCardDto(pageable)).thenReturn(cardPage);

        Page<CardDto> result = cardService.findAll(pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(cardRepository, times(1)).findAllCardDto(pageable);
    }

    // ==================== BLOCK CARD (ADMIN) ====================

    @Test
    void blockCard_ShouldBlockCard_WhenCardIsActive() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(testCard));

        cardService.blockCard(cardId);

        assertThat(testCard.getStatus()).isEqualTo(CardStatus.BLOCKED);
        verify(cardRepository, times(1)).findById(cardId);
    }

    @Test
    void blockCard_ShouldThrowNotFoundException_WhenCardNotFound() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.blockCard(cardId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void blockCard_ShouldThrowInvalidOperationException_WhenCardAlreadyBlocked() {
        testCard.setStatus(CardStatus.BLOCKED);
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(testCard));

        assertThatThrownBy(() -> cardService.blockCard(cardId))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("уже заблокирована");
    }

    @Test
    void blockCard_ShouldThrowInvalidOperationException_WhenCardExpired() {
        testCard.setStatus(CardStatus.EXPIRED);
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(testCard));

        assertThatThrownBy(() -> cardService.blockCard(cardId))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Нельзя заблокировать просроченную карту");
    }

    // ==================== ACTIVATE CARD (ADMIN) ====================

    @Test
    void activate_ShouldActivateCard_WhenCardIsBlocked() {
        testCard.setStatus(CardStatus.BLOCKED);
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(testCard));

        cardService.activate(cardId);

        assertThat(testCard.getStatus()).isEqualTo(CardStatus.ACTIVE);
    }

    @Test
    void activate_ShouldThrowAccessDeniedException_WhenCardIsNotBlocked() {
        testCard.setStatus(CardStatus.ACTIVE);
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(testCard));

        assertThatThrownBy(() -> cardService.activate(cardId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Активировать можно только заблокированную карту");
    }

    // ==================== DELETE CARD (ADMIN) ====================

    @Test
    void delete_ShouldDeleteCard_WhenCardExists() {
        when(cardRepository.existsById(cardId)).thenReturn(true);
        doNothing().when(cardRepository).deleteById(cardId);

        cardService.delete(cardId);

        verify(cardRepository, times(1)).deleteById(cardId);
    }

    @Test
    void delete_ShouldThrowNotFoundException_WhenCardNotFound() {
        when(cardRepository.existsById(cardId)).thenReturn(false);

        assertThatThrownBy(() -> cardService.delete(cardId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("не найдена");
    }

    // ==================== REQUEST TO BLOCK (USER) ====================

    @Test
    void requestToBlock_ShouldSetPendingBlock_WhenCardIsActiveAndUserIsOwner() {
        when(userRepository.existsById(userId)).thenReturn(true);
        when(cardRepository.findByIdWithUser(cardId)).thenReturn(Optional.of(testCard));

        cardService.requestToBlock(cardId, userId);

        assertThat(testCard.getStatus()).isEqualTo(CardStatus.PENDING_BLOCK);
        verify(cardRepository, times(1)).findByIdWithUser(cardId);
    }

    @Test
    void requestToBlock_ShouldThrowNotFoundException_WhenUserNotFound() {
        when(userRepository.existsById(userId)).thenReturn(false);

        assertThatThrownBy(() -> cardService.requestToBlock(cardId, userId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("не найден");
    }

    @Test
    void requestToBlock_ShouldThrowNotFoundException_WhenCardNotFound() {
        when(userRepository.existsById(userId)).thenReturn(true);
        when(cardRepository.findByIdWithUser(cardId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.requestToBlock(cardId, userId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("не найдена");
    }

    @Test
    void requestToBlock_ShouldThrowAccessDeniedException_WhenUserIsNotOwner() {
        User otherUser = new User();
        otherUser.setId(999L);
        testCard.setUser(otherUser);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(cardRepository.findByIdWithUser(cardId)).thenReturn(Optional.of(testCard));

        assertThatThrownBy(() -> cardService.requestToBlock(cardId, userId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("только владелец карты");
    }

    @Test
    void requestToBlock_ShouldThrowInvalidOperationException_WhenCardNotActive() {
        testCard.setStatus(CardStatus.BLOCKED);
        when(userRepository.existsById(userId)).thenReturn(true);
        when(cardRepository.findByIdWithUser(cardId)).thenReturn(Optional.of(testCard));

        assertThatThrownBy(() -> cardService.requestToBlock(cardId, userId))
                .isInstanceOf(InvalidOperationException.class);
    }

    // ==================== FIND ALL BY USER ID ====================

    @Test
    void findAllByUserId_ShouldReturnCards_WhenStatusIsNull() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<CardDto> cardPage = new PageImpl<>(List.of(testCardDto), pageable, 1);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(cardRepository.findAllCardDtoByUserId(userId, pageable)).thenReturn(cardPage);

        Page<CardDto> result = cardService.findAllByUserId(userId, null, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(cardRepository, times(1)).findAllCardDtoByUserId(userId, pageable);
    }

    @Test
    void findAllByUserId_ShouldReturnFilteredCards_WhenStatusIsProvided() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<CardDto> cardPage = new PageImpl<>(List.of(testCardDto), pageable, 1);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(cardRepository.findAllCardDtoByUserIdAndStatus(userId, CardStatus.ACTIVE, pageable))
                .thenReturn(cardPage);

        Page<CardDto> result = cardService.findAllByUserId(userId, CardStatus.ACTIVE, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(cardRepository, times(1))
                .findAllCardDtoByUserIdAndStatus(userId, CardStatus.ACTIVE, pageable);
    }

    @Test
    void findAllByUserId_ShouldThrowNotFoundException_WhenUserNotFound() {
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.existsById(userId)).thenReturn(false);

        assertThatThrownBy(() -> cardService.findAllByUserId(userId, null, pageable))
                .isInstanceOf(NotFoundException.class);
    }

    // ==================== GET BALANCE ====================

    @Test
    void getBalance_ShouldReturnBalance_WhenUserIsOwner() {
        when(userRepository.existsById(userId)).thenReturn(true);
        when(cardRepository.findCardDtoById(cardId)).thenReturn(Optional.of(testCardDto));

        BigDecimal result = cardService.getBalance(cardId, userId);

        assertThat(result).isEqualTo(balance);
    }

    @Test
    void getBalance_ShouldThrowNotFoundException_WhenUserNotFound() {
        when(userRepository.existsById(userId)).thenReturn(false);

        assertThatThrownBy(() -> cardService.getBalance(cardId, userId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getBalance_ShouldThrowAccessDeniedException_WhenUserIsNotOwner() {
        CardDto otherUserCardDto = CardDto.builder()
                .id(cardId)
                .ownerId(999L)
                .balance(balance)
                .build();

        when(userRepository.existsById(userId)).thenReturn(true);
        when(cardRepository.findCardDtoById(cardId)).thenReturn(Optional.of(otherUserCardDto));

        assertThatThrownBy(() -> cardService.getBalance(cardId, userId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("только владелец карты");
    }
}