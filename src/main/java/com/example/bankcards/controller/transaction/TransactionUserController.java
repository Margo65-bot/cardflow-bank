package com.example.bankcards.controller.transaction;

import com.example.bankcards.dto.transaction.TransactionDto;
import com.example.bankcards.dto.transaction.TransferRequest;
import com.example.bankcards.security.CustomUserDetails;
import com.example.bankcards.service.transaction.TransactionUserService;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
@Tag(name = "Transfers (User)", description = "Переводы для пользователей")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('USER')")
public class TransactionUserController {
    private final TransactionUserService transactionService;

    @PostMapping
    @Operation(summary = "Перевод между своими картами")
    public ResponseEntity<TransactionDto> transfer(
            @Valid @RequestBody TransferRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("USER: Запрос на перевод: fromCardId={}, toCardId={}, amount={}, userId={}",
                request.fromCardId(), request.toCardId(), request.amount(), userDetails.getUserId()
        );

        TransactionDto transaction = transactionService.transferBetweenOwnCards(request, userDetails.getUserId());

        return ResponseEntity.status(HttpStatus.CREATED).body(transaction);
    }

    @GetMapping("/history/card/{cardId}")
    @Operation(summary = "История переводов по карте")
    public ResponseEntity<Page<TransactionDto>> getTransactionHistory(
            @PathVariable Long cardId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("USER: Запрос истории переводов: cardId={}, userId={}, page={}, size={}",
                cardId, userDetails.getUserId(), pageable.getPageNumber(), pageable.getPageSize()
        );

        return ResponseEntity.ok(transactionService.getTransactionHistory(cardId, userDetails.getUserId(), pageable));
    }
}
