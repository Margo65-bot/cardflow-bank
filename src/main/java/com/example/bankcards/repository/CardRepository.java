package com.example.bankcards.repository;

import com.example.bankcards.dto.card.CardDto;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.enums.CardStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long> {
    @Query("""
                SELECT new com.example.bankcards.dto.card.CardDto(
                    c.id,
                    c.cardNumber,
                    c.user.id,
                    c.expiryDate,
                    c.status,
                    c.balance
                )
                FROM Card c
                WHERE c.id = :id
            """)
    Optional<CardDto> findCardDtoById(@PathVariable Long id);

    @Query("""
                SELECT new com.example.bankcards.dto.card.CardDto(
                    c.id,
                    c.cardNumber,
                    c.user.id,
                    c.expiryDate,
                    c.status,
                    c.balance
                )
                FROM Card c
                WHERE c.user.id = :userId
            """)
    Page<CardDto> findAllCardDtoByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("""
                SELECT new com.example.bankcards.dto.card.CardDto(
                    c.id,
                    c.cardNumber,
                    c.user.id,
                    c.expiryDate,
                    c.status,
                    c.balance
                )
                FROM Card c
                WHERE c.user.id = :userId
                AND c.status = :status
            """)
    Page<CardDto> findAllCardDtoByUserIdAndStatus(
            @Param("userId") Long userId,
            @Param("status") CardStatus status,
            Pageable pageable
    );

    @Query("""
                SELECT new com.example.bankcards.dto.card.CardDto(
                    c.id,
                    c.cardNumber,
                    c.user.id,
                    c.expiryDate,
                    c.status,
                    c.balance
                )
                FROM Card c
            """)
    Page<CardDto> findAllCardDto(Pageable pageable);

    @Query("""
            SELECT c FROM Card c
            JOIN FETCH c.user
            WHERE c.id = :cardId
            """)
    Optional<Card> findByIdWithUser(@PathVariable Long cardId);

    @Query("""
            SELECT c FROM Card c
            WHERE c.status IN :statuses
            """)
    List<Card> findByStatusIn(@Param("statuses") List<CardStatus> statuses);

    boolean existsByCardNumber(String cardNumber);

    boolean existsByUserId(Long userId);
}
