package com.example.bankcards.dto.card;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

public record CreateCardRequest(
        @NotBlank(message = "Номер карты обязателен")
        @Pattern(regexp = "\\d{16}", message = "Номер карты должен содержать 16 цифр")
        String cardNumber,

        @NotBlank(message = "Срок действия карты обязателен")
        @Pattern(regexp = "(0[1-9]|1[0-2])/\\d{2}", message = "Срок действия должен быть в формате ММ/ГГ")
        String expiryDate,

        @NotNull(message = "ID пользователя обязателен")
        Long userId,

        @NotNull(message = "Начальный баланс обязателен")
        @DecimalMin(value = "0.00", message = "Баланс не может быть отрицательным")
        BigDecimal balance
) {
        @JsonIgnore
        public LocalDate getExpiryDateToLocalDate() {
                YearMonth yearMonth = YearMonth.parse(expiryDate, DateTimeFormatter.ofPattern("MM/yy"));
                return yearMonth.atEndOfMonth();
        }
}