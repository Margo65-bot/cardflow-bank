package com.example.bankcards.controller.card;

import com.example.bankcards.dto.card.CardDto;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.security.CustomUserDetails;
import com.example.bankcards.service.card.CardUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Slf4j
@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
@Tag(name = "Cards (User)", description = "Операции с картами для пользователей")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('USER')")
public class CardUserController {
    private final CardUserService cardService;

    @GetMapping("/me")
    @Operation(summary = "Мои карты")
    public ResponseEntity<Page<CardDto>> getMyCards(
            @RequestParam(required = false) CardStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("USER: Запрос списка карт пользователя: userId={}, status={}, page={}, size={}",
                userDetails.getUserId(), status, pageable.getPageNumber(), pageable.getPageSize()
        );

        return ResponseEntity.ok(cardService.findAllByUserId(userDetails.getUserId(), status, pageable));
    }

    @GetMapping("/{id}/balance")
    @Operation(summary = "Баланс карты")
    public ResponseEntity<BigDecimal> getBalance(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("USER: Запрос баланса карты: cardId={}, userId={}", id, userDetails.getUserId());

        return ResponseEntity.ok(cardService.getBalance(id, userDetails.getUserId()));
    }

    @PostMapping("/{id}/block-request")
    @Operation(summary = "Запросить блокировку карты")
    public ResponseEntity<String> requestBlockCard(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("USER: Запрос на блокировку карты: cardId={}, userId={}", id, userDetails.getUserId());

        cardService.requestToBlock(id, userDetails.getUserId());

        return ResponseEntity.accepted().body("Block request submitted for card " + id);
    }
}
