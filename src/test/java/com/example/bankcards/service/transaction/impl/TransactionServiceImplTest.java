package com.example.bankcards.service.transaction.impl;

import com.example.bankcards.dto.transaction.TransactionDto;
import com.example.bankcards.dto.transaction.TransferRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Transaction;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.entity.enums.Role;
import com.example.bankcards.entity.enums.TransactionStatus;
import com.example.bankcards.exception.AccessDeniedException;
import com.example.bankcards.exception.InsufficientFundsException;
import com.example.bankcards.exception.InvalidOperationException;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransactionRepository;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {
    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CardRepository cardRepository;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private User testUser;
    private Card fromCard;
    private Card toCard;
    private Transaction transaction;
    private TransactionDto transactionDto;

    private final Long userId = 1L;
    private final Long fromCardId = 1L;
    private final Long toCardId = 2L;
    private final BigDecimal amount = new BigDecimal("100.00");
    private final BigDecimal fromCardBalance = new BigDecimal("500.00");
    private final BigDecimal toCardBalance = new BigDecimal("200.00");

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(userId);
        testUser.setUsername("testuser");
        testUser.setRole(Role.USER);

        fromCard = new Card();
        fromCard.setId(fromCardId);
        fromCard.setCardNumber("1234567890123456");
        fromCard.setStatus(CardStatus.ACTIVE);
        fromCard.setBalance(fromCardBalance);
        fromCard.setUser(testUser);

        toCard = new Card();
        toCard.setId(toCardId);
        toCard.setCardNumber("6543210987654321");
        toCard.setStatus(CardStatus.ACTIVE);
        toCard.setBalance(toCardBalance);
        toCard.setUser(testUser);

        transaction = new Transaction();
        transaction.setId(1L);
        transaction.setFromCard(fromCard);
        transaction.setToCard(toCard);
        transaction.setAmount(amount);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setCreatedAt(LocalDateTime.now());

        transactionDto = TransactionDto.builder()
                .id(1L)
                .fromCardId(fromCardId)
                .toCardId(toCardId)
                .amount(amount)
                .status(TransactionStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ==================== TRANSFER BETWEEN OWN CARDS ====================

    @Test
    void transferBetweenOwnCards_ShouldTransferSuccessfully() {
        TransferRequest request = new TransferRequest(fromCardId, toCardId, amount);

        when(cardRepository.findByIdWithUser(fromCardId)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByIdWithUser(toCardId)).thenReturn(Optional.of(toCard));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);

        TransactionDto result = transactionService.transferBetweenOwnCards(request, userId);

        assertThat(result).isNotNull();
        assertThat(result.getAmount()).isEqualTo(amount);
        assertThat(result.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(fromCard.getBalance()).isEqualTo(fromCardBalance.subtract(amount));
        assertThat(toCard.getBalance()).isEqualTo(toCardBalance.add(amount));

        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void transferBetweenOwnCards_ShouldThrowInvalidOperationException_WhenSameCard() {
        TransferRequest request = new TransferRequest(fromCardId, fromCardId, amount);

        assertThatThrownBy(() -> transactionService.transferBetweenOwnCards(request, userId))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Нельзя перевести деньги на ту же карту");

        verify(cardRepository, never()).findByIdWithUser(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transferBetweenOwnCards_ShouldThrowNotFoundException_WhenFromCardNotFound() {
        TransferRequest request = new TransferRequest(999L, toCardId, amount);

        when(cardRepository.findByIdWithUser(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.transferBetweenOwnCards(request, userId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Карта списания не найдена");
    }

    @Test
    void transferBetweenOwnCards_ShouldThrowNotFoundException_WhenToCardNotFound() {
        TransferRequest request = new TransferRequest(fromCardId, 999L, amount);

        when(cardRepository.findByIdWithUser(fromCardId)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByIdWithUser(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.transferBetweenOwnCards(request, userId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Карта зачисления не найдена");
    }

    @Test
    void transferBetweenOwnCards_ShouldThrowAccessDeniedException_WhenFromCardNotBelongsToUser() {
        User otherUser = new User();
        otherUser.setId(999L);
        fromCard.setUser(otherUser);

        TransferRequest request = new TransferRequest(fromCardId, toCardId, amount);

        when(cardRepository.findByIdWithUser(fromCardId)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByIdWithUser(toCardId)).thenReturn(Optional.of(toCard));

        assertThatThrownBy(() -> transactionService.transferBetweenOwnCards(request, userId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("не принадлежит пользователю");
    }

    @Test
    void transferBetweenOwnCards_ShouldThrowAccessDeniedException_WhenToCardNotBelongsToUser() {
        User otherUser = new User();
        otherUser.setId(999L);
        toCard.setUser(otherUser);

        TransferRequest request = new TransferRequest(fromCardId, toCardId, amount);

        when(cardRepository.findByIdWithUser(fromCardId)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByIdWithUser(toCardId)).thenReturn(Optional.of(toCard));

        assertThatThrownBy(() -> transactionService.transferBetweenOwnCards(request, userId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("не принадлежит пользователю");
    }

    @Test
    void transferBetweenOwnCards_ShouldThrowInvalidOperationException_WhenFromCardNotActive() {
        fromCard.setStatus(CardStatus.BLOCKED);
        TransferRequest request = new TransferRequest(fromCardId, toCardId, amount);

        when(cardRepository.findByIdWithUser(fromCardId)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByIdWithUser(toCardId)).thenReturn(Optional.of(toCard));

        assertThatThrownBy(() -> transactionService.transferBetweenOwnCards(request, userId))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Карта списания неактивна или заблокирована");
    }

    @Test
    void transferBetweenOwnCards_ShouldThrowInvalidOperationException_WhenToCardNotActive() {
        toCard.setStatus(CardStatus.BLOCKED);
        TransferRequest request = new TransferRequest(fromCardId, toCardId, amount);

        when(cardRepository.findByIdWithUser(fromCardId)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByIdWithUser(toCardId)).thenReturn(Optional.of(toCard));

        assertThatThrownBy(() -> transactionService.transferBetweenOwnCards(request, userId))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Карта зачисления неактивна или заблокирована");
    }

    @Test
    void transferBetweenOwnCards_ShouldThrowInsufficientFundsException_WhenBalanceNotEnough() {
        BigDecimal largeAmount = new BigDecimal("1000.00");
        TransferRequest request = new TransferRequest(fromCardId, toCardId, largeAmount);

        when(cardRepository.findByIdWithUser(fromCardId)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByIdWithUser(toCardId)).thenReturn(Optional.of(toCard));

        assertThatThrownBy(() -> transactionService.transferBetweenOwnCards(request, userId))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("Недостаточно средств");
    }

    // ==================== GET TRANSACTION HISTORY ====================

    @Test
    void getTransactionHistory_ShouldReturnPageOfTransactions_WhenUserIsOwner() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<TransactionDto> transactionPage = new PageImpl<>(List.of(transactionDto), pageable, 1);

        when(cardRepository.findByIdWithUser(fromCardId)).thenReturn(Optional.of(fromCard));
        when(transactionRepository.findTransactionHistoryByCardId(fromCardId, pageable))
                .thenReturn(transactionPage);

        Page<TransactionDto> result = transactionService.getTransactionHistory(fromCardId, userId, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(1L);
        verify(transactionRepository, times(1))
                .findTransactionHistoryByCardId(fromCardId, pageable);
    }

    @Test
    void getTransactionHistory_ShouldThrowNotFoundException_WhenCardNotFound() {
        Pageable pageable = PageRequest.of(0, 10);

        when(cardRepository.findByIdWithUser(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.getTransactionHistory(999L, userId, pageable))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Карта не найдена");
    }

    @Test
    void getTransactionHistory_ShouldThrowAccessDeniedException_WhenUserIsNotOwner() {
        User otherUser = new User();
        otherUser.setId(999L);
        fromCard.setUser(otherUser);
        Pageable pageable = PageRequest.of(0, 10);

        when(cardRepository.findByIdWithUser(fromCardId)).thenReturn(Optional.of(fromCard));

        assertThatThrownBy(() -> transactionService.getTransactionHistory(fromCardId, userId, pageable))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("доступна только владельцу карты");
    }

    // ==================== GET ALL TRANSACTIONS (ADMIN) ====================

    @Test
    void getAllTransactions_ShouldReturnPageOfTransactions() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<TransactionDto> transactionPage = new PageImpl<>(List.of(transactionDto), pageable, 1);

        when(transactionRepository.findAllTransactionDto(pageable)).thenReturn(transactionPage);

        Page<TransactionDto> result = transactionService.getAllTransactions(pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(1L);
        verify(transactionRepository, times(1)).findAllTransactionDto(pageable);
    }
}