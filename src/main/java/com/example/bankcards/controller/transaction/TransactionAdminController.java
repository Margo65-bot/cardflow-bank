package com.example.bankcards.controller.transaction;

import com.example.bankcards.dto.transaction.TransactionDto;
import com.example.bankcards.service.transaction.TransactionAdminService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
@Tag(name = "Transfers (Admin)", description = "Просмотр переводов для администратора")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class TransactionAdminController {
    private final TransactionAdminService transactionService;

    @GetMapping
    @Operation(summary = "Все переводы")
    public ResponseEntity<Page<TransactionDto>> getAllTransactions(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        log.info("ADMIN: Запрос всех переводов от администратора: page={}, size={}",
                pageable.getPageNumber(), pageable.getPageSize()
        );

        return ResponseEntity.ok(transactionService.getAllTransactions(pageable));
    }
}