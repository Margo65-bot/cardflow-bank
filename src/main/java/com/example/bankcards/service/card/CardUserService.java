package com.example.bankcards.service.card;

import com.example.bankcards.dto.card.CardDto;
import com.example.bankcards.entity.enums.CardStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface CardUserService {
    Page<CardDto> findAllByUserId(Long userId, CardStatus status, Pageable pageable);

    void requestToBlock(Long cardId, Long userId);

    BigDecimal getBalance(Long cardId, Long userId);
}
