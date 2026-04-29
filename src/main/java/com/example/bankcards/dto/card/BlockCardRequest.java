package com.example.bankcards.dto.card;

import jakarta.validation.constraints.NotNull;

public record BlockCardRequest(
        @NotNull(message = "ID карты обязателен")
        Long cardId
) {}