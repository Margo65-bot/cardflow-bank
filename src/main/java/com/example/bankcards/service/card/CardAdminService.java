package com.example.bankcards.service.card;

import com.example.bankcards.dto.card.CardDto;
import com.example.bankcards.dto.card.CreateCardRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CardAdminService {
    CardDto create(CreateCardRequest request);

    Page<CardDto> findAll(Pageable pageable);

    CardDto findById(Long cardId);

    void blockCard(Long cardId);

    void activate(Long cardId);

    void delete(Long cardId);
}
