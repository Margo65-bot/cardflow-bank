package com.example.bankcards.dto.card;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.enums.CardStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class CardDto {
    private Long id;
    private String maskedNumber;
    private Long ownerId;
    private String expiryDate;
    private CardStatus status;
    private BigDecimal balance;

    public CardDto(Long id, String cardNumber, Long ownerId, String expiryDate, CardStatus status, BigDecimal balance) {
        this.id = id;
        this.maskedNumber = maskCardNumber(cardNumber);
        this.ownerId = ownerId;
        this.expiryDate = expiryDate;
        this.status = status;
        this.balance = balance;
    }
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

    private static String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) return "****";
        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
    }
}
