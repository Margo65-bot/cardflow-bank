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

/**
 * Сущность банковской карты.
 *
 * <p>Номер карты хранится в зашифрованном виде (AES).
 * При чтении автоматически расшифровывается через {@link CardNumberEncryptionConverter}.</p>
 *
 * <p>Особенности:</p>
 * <ul>
 *   <li>Номер карты уникален в системе</li>
 *   <li>Баланс не может быть отрицательным (контролируется на уровне приложения)</li>
 *   <li>При удалении пользователя все его карты удаляются каскадно</li>
 * </ul>
 *
 * <p>Связи:</p>
 * <ul>
 *   <li>Many-to-One к {@link User} (владелец карты)</li>
 *   <li>One-to-Many к {@link Transaction} (исходящие и входящие переводы)</li>
 * </ul>
 *
 * @see CardStatus
 * @see CardNumberEncryptionConverter
 */
@Entity
@Table(name = "cards")
@Getter
@Setter
@ToString
public class Card {
    /** Уникальный идентификатор карты, генерируется автоматически */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Зашифрованный номер карты (16 цифр).
     * Исключён из {@code toString()} — не должен попадать в логи.
     */
    @Convert(converter = CardNumberEncryptionConverter.class)
    @Column(name = "card_number", nullable = false, unique = true)
    @ToString.Exclude
    private String cardNumber;

    /** Срок действия карты в формате {@code ММ/ГГ} (например, {@code 12/28}) */
    @Column(name = "expiry_date", nullable = false, length = 7)
    private String expiryDate;

    /**
     * Текущий статус карты.
     * Возможные значения: {@code ACTIVE}, {@code BLOCKED}, {@code EXPIRED}, {@code PENDING_BLOCK}
     */
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private CardStatus status;

    /** Текущий баланс карты. Точность: 15 знаков, 2 после запятой */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance;

    /** Владелец карты. Загружается лениво (LAZY) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    /** Дата и время создания карты */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * Возвращает замаскированный номер карты для отображения.
     *
     * <p>Видны только последние 4 цифры, остальные заменены на звёздочки.
     * Пример: {@code "1234567890123456" → "**** **** **** 3456"}</p>
     *
     * @return замаскированный номер или {@code "****"} если номер null или короче 4 символов
     */
    public String getMaskedNumber() {
        if (cardNumber == null || cardNumber.length() < 4) return "****";
        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
    }
}
