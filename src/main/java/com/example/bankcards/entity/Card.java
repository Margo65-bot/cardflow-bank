package com.example.bankcards.entity;

import com.example.bankcards.util.CardNumberEncryptionConverter;
import com.example.bankcards.entity.enums.CardStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cards")
@Getter
@Setter
@ToString
public class Card {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Convert(converter = CardNumberEncryptionConverter.class)
    @Column(name = "card_number", nullable = false, unique = true)
    @ToString.Exclude
    private String cardNumber;

    @Column(name = "expiry_date", nullable = false, length = 7)
    private String expiryDate;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private CardStatus status;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public String getMaskedNumber() {
        if (cardNumber == null || cardNumber.length() < 4) return "****";
        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
    }
}
