package com.example.bankcards.controller.card;

import com.example.bankcards.dto.card.CardDto;
import com.example.bankcards.dto.card.CreateCardRequest;
import com.example.bankcards.service.card.CardAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
@Tag(name = "Cards (Admin)", description = "Администрирование карт")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class CardAdminController {
    private final CardAdminService cardService;

    @GetMapping
    @Operation(summary = "Все карты")
    public ResponseEntity<Page<CardDto>> getAllCards(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        log.info("ADMIN: Запрос всех карт от администратора: page={}, size={}",
                pageable.getPageNumber(), pageable.getPageSize()
        );

        return ResponseEntity.ok(cardService.findAll(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Карта по ID (админ)")
    public ResponseEntity<CardDto> getCardById(
            @PathVariable Long id
    ) {
        log.info("ADMIN: Запрос карты по ID от администратора: cardId={}", id);

        return ResponseEntity.ok(cardService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Создать карту")
    public ResponseEntity<CardDto> createCard(
            @Valid @RequestBody CreateCardRequest request
    ) {
        log.info("Запрос на создание карты администратором: userId={}, cardNumber={}, balance={}",
                request.userId(), request.cardNumber(), request.balance()
        );

        CardDto created = cardService.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header("Location", "/api/cards/" + created.getId())
                .body(created);
    }

    @PutMapping("/{id}/block")
    @Operation(summary = "Заблокировать карту")
    public ResponseEntity<Void> blockCard(
            @PathVariable Long id
    ) {
        log.info("Запрос на блокировку карты администратором: cardId={}", id);

        cardService.blockCard(id);

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/activate")
    @Operation(summary = "Активировать карту")
    public ResponseEntity<Void> activateCard(
            @PathVariable Long id
    ) {
        log.info("Запрос на активацию карты администратором: cardId={}", id);

        cardService.activate(id);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить карту")
    public ResponseEntity<Void> deleteCard(
            @PathVariable Long id
    ) {
        log.info("Запрос на удаление карты администратором: cardId={}", id);

        cardService.delete(id);

        return ResponseEntity.noContent().build();
    }
}
