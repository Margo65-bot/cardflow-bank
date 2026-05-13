package com.example.bankcards.dto.card;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.enums.CardStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * DTO для передачи данных банковской карты клиенту.
 *
 * <p>Номер карты автоматически маскируется при создании —
 * отображаются только последние 4 цифры.</p>
 *
 * <p>Пример маскирования: {@code 1234 5678 9012 3456 → **** **** **** 3456}</p>
 */
@Getter
@Builder
public class CardDto {
    /** Уникальный идентификатор карты */
    private Long id;

    /** Замаскированный номер карты (видны только последние 4 цифры) */
    private String maskedNumber;

    /** ID владельца карты */
    private Long ownerId;

    /** Срок действия в формате {@code ММ/ГГ} */
    private String expiryDate;

    /** Текущий статус карты (ACTIVE, BLOCKED, EXPIRED, PENDING_BLOCK) */
    private CardStatus status;

    /** Текущий баланс карты */
    private BigDecimal balance;

    /**
     * Создаёт DTO с автоматическим маскированием номера карты.
     *
     * @param id         идентификатор карты
     * @param cardNumber полный номер карты (16 цифр) — будет замаскирован
     * @param ownerId    ID владельца
     * @param expiryDate срок действия
     * @param status     статус карты
     * @param balance    баланс
     */
    public CardDto(Long id, String cardNumber, Long ownerId, String expiryDate, CardStatus status, BigDecimal balance) {
        this.id = id;
        this.maskedNumber = maskCardNumber(cardNumber);
        this.ownerId = ownerId;
        this.expiryDate = expiryDate;
        this.status = status;
        this.balance = balance;
    }

    /**
     * Создаёт DTO из сущности {@link Card}.
     *
     * @param card сущность карты из БД
     * @return готовый DTO или {@code null}, если {@code card == null}
     */
    public static CardDto fromEntity(Card card) {
        if (card == null) return null;
        return CardDto.builder()
                .id(card.getId())
                .maskedNumber(card.getMaskedNumber())
                .ownerId(card.getUser().getId())
                .expiryDate(card.getExpiryDate())
                .status(card.getStatus())
                .balance(card.getBalance())
                .build();
    }

    /**
     * Маскирует номер карты, оставляя видимыми только последние 4 цифры.
     *
     * <p>Пример: {@code "1234567890123456" → "**** **** **** 3456"}</p>
     *
     * @param cardNumber полный номер карты
     * @return замаскированный номер или {@code "****"}, если номер null или короче 4 символов
     */
    private static String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) return "****";
        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
    }
}
