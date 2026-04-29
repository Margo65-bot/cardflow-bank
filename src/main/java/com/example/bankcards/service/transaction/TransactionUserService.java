package com.example.bankcards.service.transaction;

import com.example.bankcards.dto.transaction.TransactionDto;
import com.example.bankcards.dto.transaction.TransferRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TransactionUserService {
    TransactionDto transferBetweenOwnCards(TransferRequest request, Long userId);

    Page<TransactionDto> getTransactionHistory(Long cardId, Long userId, Pageable pageable);
}