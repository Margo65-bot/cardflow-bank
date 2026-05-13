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

/**
 * DTO для запроса на создание новой банковской карты.
 *
 * <p>Все поля обязательны. Номер карты проверяется на 16 цифр,
 * срок действия — на формат {@code ММ/ГГ}.</p>
 *
 * @param cardNumber номер карты (ровно 16 цифр, без пробелов)
 * @param expiryDate срок действия в формате {@code ММ/ГГ} (например, {@code 12/28})
 * @param userId     ID пользователя-владельца
 * @param balance    начальный баланс (не может быть отрицательным)
 */
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
        /**
         * Преобразует строковый срок действия в {@link LocalDate}.
         *
         * <p>Берётся последний день указанного месяца.
         * Например, {@code "12/28"} → {@code 2028-12-31}.</p>
         *
         * @return дата истечения срока действия (последний день месяца)
         */
        @JsonIgnore
        public LocalDate getExpiryDateToLocalDate() {
                YearMonth yearMonth = YearMonth.parse(expiryDate, DateTimeFormatter.ofPattern("MM/yy"));
                return yearMonth.atEndOfMonth();
        }
}