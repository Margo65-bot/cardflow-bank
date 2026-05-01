package com.example.bankcards.controller.transaction;

import com.example.bankcards.config.SecurityConfig;
import com.example.bankcards.dto.transaction.TransactionDto;
import com.example.bankcards.entity.enums.TransactionStatus;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.CustomUserDetails;
import com.example.bankcards.security.CustomUserDetailsService;
import com.example.bankcards.security.JwtRequestFilter;
import com.example.bankcards.service.transaction.TransactionAdminService;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionAdminController.class)
@Import({SecurityConfig.class, JwtUtil.class, JwtRequestFilter.class, CustomUserDetailsService.class})
public class TransactionAdminControllerTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @MockitoBean
    private TransactionAdminService transactionService;

    @MockitoBean
    private UserRepository userRepository;

    private final Long adminUserId = 1L;
    private final String adminUsername = "admin";
    private final String adminEmail = "admin@example.com";

    @BeforeEach
    void setUp() {
        CustomUserDetails adminDetails = new CustomUserDetails(
                adminUserId,
                adminUsername,
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(adminDetails, null, adminDetails.getAuthorities());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }

    @Test
    void getAllTransactions_shouldReturnPageOfTransactions() throws Exception {
        TransactionDto transactionDto = TransactionDto.builder()
                .id(1L)
                .fromCardId(10L)
                .toCardId(20L)
                .amount(new BigDecimal("100.00"))
                .status(TransactionStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();

        Page<TransactionDto> page = new PageImpl<>(List.of(transactionDto), PageRequest.of(0, 20), 1);

        when(transactionService.getAllTransactions(any(Pageable.class))).thenReturn(page);

        mvc.perform(get("/api/transfers")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].amount").value(100.00))
                .andExpect(jsonPath("$.content[0].status").value("SUCCESS"));
    }

    @Test
    void getAllTransactions_shouldReturnForbidden_whenNoAuth() throws Exception {
        SecurityContextHolder.clearContext();

        mvc.perform(get("/api/transfers"))
                .andExpect(status().isForbidden());
    }
}