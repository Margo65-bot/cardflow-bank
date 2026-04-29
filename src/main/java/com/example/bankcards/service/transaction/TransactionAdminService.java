package com.example.bankcards.service.transaction;

import com.example.bankcards.dto.transaction.TransactionDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TransactionAdminService {
    Page<TransactionDto> getAllTransactions(Pageable pageable);
}