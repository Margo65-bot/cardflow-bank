package com.example.bankcards.controller.transaction;

import com.example.bankcards.config.SecurityConfig;
import com.example.bankcards.dto.transaction.TransactionDto;
import com.example.bankcards.dto.transaction.TransferRequest;
import com.example.bankcards.entity.enums.TransactionStatus;
import com.example.bankcards.exception.AccessDeniedException;
import com.example.bankcards.exception.InsufficientFundsException;
import com.example.bankcards.exception.InvalidOperationException;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.CustomUserDetails;
import com.example.bankcards.security.CustomUserDetailsService;
import com.example.bankcards.security.JwtRequestFilter;
import com.example.bankcards.service.transaction.TransactionUserService;
import com.example.bankcards.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionUserController.class)
@Import({SecurityConfig.class, JwtUtil.class, JwtRequestFilter.class, CustomUserDetailsService.class})
public class TransactionUserControllerTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @MockitoBean
    private TransactionUserService transactionService;

    @MockitoBean
    private UserRepository userRepository;

    private final Long userId = 1L;
    private final String username = "testuser";

    @BeforeEach
    void setUp() {
        CustomUserDetails userDetails = new CustomUserDetails(
                userId,
                username,
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }

    // ==================== TRANSFER ====================

    @Test
    void transfer_shouldReturnCreated_whenValid() throws Exception {
        TransferRequest request = new TransferRequest(1L, 2L, new BigDecimal("100.00"));

        TransactionDto transactionDto = TransactionDto.builder()
                .id(1L)
                .fromCardId(1L)
                .toCardId(2L)
                .amount(new BigDecimal("100.00"))
                .status(TransactionStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();

        when(transactionService.transferBetweenOwnCards(any(TransferRequest.class), eq(userId)))
                .thenReturn(transactionDto);

        mvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void transfer_shouldReturnBadRequest_whenAmountIsNegative() throws Exception {
        TransferRequest request = new TransferRequest(1L, 2L, new BigDecimal("-50.00"));

        mvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(transactionService, never()).transferBetweenOwnCards(any(), any());
    }

    @Test
    void transfer_shouldReturnBadRequest_whenAmountIsZero() throws Exception {
        TransferRequest request = new TransferRequest(1L, 2L, BigDecimal.ZERO);

        mvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(transactionService, never()).transferBetweenOwnCards(any(), any());
    }

    @Test
    void transfer_shouldReturnBadRequest_whenFromCardIsNull() throws Exception {
        TransferRequest request = new TransferRequest(null, 2L, new BigDecimal("100.00"));

        mvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(transactionService, never()).transferBetweenOwnCards(any(), any());
    }

    @Test
    void transfer_shouldReturnBadRequest_whenToCardIsNull() throws Exception {
        TransferRequest request = new TransferRequest(1L, null, new BigDecimal("100.00"));

        mvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(transactionService, never()).transferBetweenOwnCards(any(), any());
    }

    @Test
    void transfer_shouldReturnBadRequest_whenSameCard() throws Exception {
        TransferRequest request = new TransferRequest(1L, 1L, new BigDecimal("100.00"));

        when(transactionService.transferBetweenOwnCards(any(TransferRequest.class), eq(userId)))
                .thenThrow(new InvalidOperationException("Нельзя перевести на ту же карту"));

        mvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Нельзя перевести на ту же карту"));  // ← проверяем message, не errors

        verify(transactionService, times(1)).transferBetweenOwnCards(any(TransferRequest.class), eq(userId));
    }

    @Test
    void transfer_shouldReturnNotFound_whenCardNotFound() throws Exception {
        TransferRequest request = new TransferRequest(999L, 2L, new BigDecimal("100.00"));

        when(transactionService.transferBetweenOwnCards(any(TransferRequest.class), eq(userId)))
                .thenThrow(new NotFoundException("Card not found"));

        mvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void transfer_shouldReturnBadRequest_whenInsufficientFunds() throws Exception {
        TransferRequest request = new TransferRequest(1L, 2L, new BigDecimal("1000.00"));

        when(transactionService.transferBetweenOwnCards(any(TransferRequest.class), eq(userId)))
                .thenThrow(new InsufficientFundsException("Insufficient funds"));

        mvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ==================== TRANSACTION HISTORY ====================

    @Test
    void getTransactionHistory_shouldReturnPageOfTransactions() throws Exception {
        Long cardId = 1L;

        TransactionDto transactionDto = TransactionDto.builder()
                .id(1L)
                .fromCardId(cardId)
                .toCardId(2L)
                .amount(new BigDecimal("50.00"))
                .status(TransactionStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();

        Page<TransactionDto> page = new PageImpl<>(List.of(transactionDto), PageRequest.of(0, 10), 1);

        when(transactionService.getTransactionHistory(eq(cardId), eq(userId), any(Pageable.class)))
                .thenReturn(page);

        mvc.perform(get("/api/transfers/history/card/{cardId}", cardId)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].amount").value(50.00));
    }

    @Test
    void getTransactionHistory_shouldReturnForbidden_whenCardNotBelongsToUser() throws Exception {
        Long cardId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        when(transactionService.getTransactionHistory(eq(cardId), eq(userId), any(Pageable.class)))
                .thenThrow(new AccessDeniedException("Access denied"));

        mvc.perform(get("/api/transfers/history/card/{cardId}", cardId)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getTransactionHistory_shouldReturnForbidden_whenNoAuth() throws Exception {
        SecurityContextHolder.clearContext();

        mvc.perform(get("/api/transfers/history/card/1"))
                .andExpect(status().isForbidden());
    }
}