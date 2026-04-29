package com.example.bankcards.dto.transaction;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TransferRequest(
        @NotNull(message = "ID карты списания обязателен")
        Long fromCardId,

        @NotNull(message = "ID карты зачисления обязателен")
        Long toCardId,

        @NotNull(message = "Сумма перевода обязательна")
        @DecimalMin(value = "0.01", message = "Сумма перевода должна быть больше 0")
        BigDecimal amount
) {}