package com.example.bankcards.service.transaction.impl;

import com.example.bankcards.dto.transaction.TransactionDto;
import com.example.bankcards.dto.transaction.TransferRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Transaction;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.entity.enums.TransactionStatus;
import com.example.bankcards.exception.AccessDeniedException;
import com.example.bankcards.exception.InsufficientFundsException;
import com.example.bankcards.exception.InvalidOperationException;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransactionRepository;
import com.example.bankcards.service.transaction.TransactionAdminService;
import com.example.bankcards.service.transaction.TransactionUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionServiceImpl implements TransactionUserService, TransactionAdminService {
    private final TransactionRepository transactionRepository;

    private final CardRepository cardRepository;

    @Override
    @Transactional
    public TransactionDto transferBetweenOwnCards(TransferRequest request, Long userId) {
        if (request.fromCardId().equals(request.toCardId())) {
            throw new InvalidOperationException("Нельзя перевести деньги на ту же карту");
        }

        Card fromCard = cardRepository.findByIdWithUser(request.fromCardId())
                .orElseThrow(() -> new NotFoundException("Карта списания не найдена"));

        Card toCard = cardRepository.findByIdWithUser(request.toCardId())
                .orElseThrow(() -> new NotFoundException("Карта зачисления не найдена"));

        if (!fromCard.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Карта списания не принадлежит пользователю");
        }

        if (!toCard.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Карта зачисления не принадлежит пользователю");
        }

        if (fromCard.getStatus() != CardStatus.ACTIVE) {
            throw new InvalidOperationException("Карта списания неактивна или заблокирована");
        }

        if (toCard.getStatus() != CardStatus.ACTIVE) {
            throw new InvalidOperationException("Карта зачисления неактивна или заблокирована");
        }

        BigDecimal amount = request.amount();
        if (fromCard.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Недостаточно средств на карте списания");
        }

        fromCard.setBalance(fromCard.getBalance().subtract(amount));
        toCard.setBalance(toCard.getBalance().add(amount));

        Transaction transaction = new Transaction();
        transaction.setFromCard(fromCard);
        transaction.setToCard(toCard);
        transaction.setAmount(amount);
        transaction.setStatus(TransactionStatus.SUCCESS);

        return TransactionDto.fromEntity(transactionRepository.save(transaction));
    }

    @Override
    public Page<TransactionDto> getTransactionHistory(Long cardId, Long userId, Pageable pageable) {
        Card card = cardRepository.findByIdWithUser(cardId)
                .orElseThrow(() -> new NotFoundException("Карта не найдена"));

        if (!card.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("История переводов доступна только владельцу карты");
        }
        return transactionRepository.findTransactionHistoryByCardId(cardId, pageable);
    }

    @Override
    public Page<TransactionDto> getAllTransactions(Pageable pageable) {
        return transactionRepository.findAllTransactionDto(pageable);
    }
}
