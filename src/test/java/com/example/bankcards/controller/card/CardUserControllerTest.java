package com.example.bankcards.controller.card;

import com.example.bankcards.config.SecurityConfig;
import com.example.bankcards.dto.card.CardDto;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.CustomUserDetails;
import com.example.bankcards.security.CustomUserDetailsService;
import com.example.bankcards.security.JwtRequestFilter;
import com.example.bankcards.service.card.CardUserService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CardUserController.class)
@Import({SecurityConfig.class, JwtUtil.class, JwtRequestFilter.class, CustomUserDetailsService.class})
public class CardUserControllerTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @MockitoBean
    private CardUserService cardService;

    @MockitoBean
    private UserRepository userRepository;

    private final Long userId = 1L;
    private final String username = "testuser";
    private final Long cardId = 1L;

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

    // ==================== GET MY CARDS ====================

    @Test
    void getMyCards_shouldReturnPageOfCards() throws Exception {
        CardDto cardDto = CardDto.builder()
                .id(cardId)
                .maskedNumber("**** **** **** 1234")
                .expiryDate("12/28")
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("1000.00"))
                .build();

        Page<CardDto> page = new PageImpl<>(List.of(cardDto), PageRequest.of(0, 10), 1);

        when(cardService.findAllByUserId(eq(userId), any(), any(Pageable.class))).thenReturn(page);

        mvc.perform(get("/api/cards/me")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(cardId))
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"));
    }

    @Test
    void getMyCards_withStatusFilter_shouldReturnFilteredCards() throws Exception {
        CardDto cardDto = CardDto.builder()
                .id(cardId)
                .maskedNumber("**** **** **** 1234")
                .expiryDate("12/28")
                .status(CardStatus.BLOCKED)
                .balance(new BigDecimal("1000.00"))
                .build();

        Page<CardDto> page = new PageImpl<>(List.of(cardDto), PageRequest.of(0, 10), 1);

        when(cardService.findAllByUserId(eq(userId), eq(CardStatus.BLOCKED), any(Pageable.class))).thenReturn(page);

        mvc.perform(get("/api/cards/me")
                        .param("status", "BLOCKED")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("BLOCKED"));
    }

    @Test
    void getMyCards_shouldReturnForbidden_whenNoAuth() throws Exception {
        SecurityContextHolder.clearContext();

        mvc.perform(get("/api/cards/me"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET BALANCE ====================

    @Test
    void getBalance_shouldReturnBalance_whenCardBelongsToUser() throws Exception {
        BigDecimal balance = new BigDecimal("1500.00");

        when(cardService.getBalance(cardId, userId)).thenReturn(balance);

        mvc.perform(get("/api/cards/{id}/balance", cardId))
                .andExpect(status().isOk())
                .andExpect(content().string("1500.00"));
    }

    @Test
    void getBalance_shouldReturnNotFound_whenCardDoesNotExist() throws Exception {
        when(cardService.getBalance(999L, userId)).thenThrow(new NotFoundException("Card not found"));

        mvc.perform(get("/api/cards/{id}/balance", 999L))
                .andExpect(status().isNotFound());
    }

    // ==================== REQUEST BLOCK CARD ====================

    @Test
    void requestBlockCard_shouldReturnAccepted_whenSuccess() throws Exception {
        doNothing().when(cardService).requestToBlock(cardId, userId);

        mvc.perform(post("/api/cards/{id}/block-request", cardId))
                .andExpect(status().isAccepted())
                .andExpect(content().string("Block request submitted for card " + cardId));

        verify(cardService, times(1)).requestToBlock(cardId, userId);
    }

    @Test
    void requestBlockCard_shouldReturnNotFound_whenCardDoesNotExist() throws Exception {
        doThrow(new NotFoundException("Card not found")).when(cardService).requestToBlock(999L, userId);

        mvc.perform(post("/api/cards/{id}/block-request", 999L))
                .andExpect(status().isNotFound());
    }
}