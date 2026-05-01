package com.example.bankcards.controller.card;

import com.example.bankcards.config.SecurityConfig;
import com.example.bankcards.dto.card.CardDto;
import com.example.bankcards.dto.card.CreateCardRequest;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.exception.AlreadyExistsException;
import com.example.bankcards.exception.InvalidOperationException;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.CustomUserDetails;
import com.example.bankcards.security.CustomUserDetailsService;
import com.example.bankcards.security.JwtRequestFilter;
import com.example.bankcards.service.card.CardAdminService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CardAdminController.class)
@Import({SecurityConfig.class, JwtUtil.class, JwtRequestFilter.class, CustomUserDetailsService.class})
public class CardAdminControllerTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @MockitoBean
    private CardAdminService cardService;

    @MockitoBean
    private UserRepository userRepository;

    private final Long adminUserId = 1L;
    private final String adminUsername = "admin";
    private final Long cardId = 1L;

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

    // ==================== GET ALL CARDS ====================

    @Test
    void getAllCards_shouldReturnPageOfCards() throws Exception {
        CardDto cardDto = CardDto.builder()
                .id(cardId)
                .maskedNumber("**** **** **** 1234")
                .expiryDate("12/28")
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("1000.00"))
                .build();

        Page<CardDto> page = new PageImpl<>(List.of(cardDto), PageRequest.of(0, 10), 1);

        when(cardService.findAll(any(Pageable.class))).thenReturn(page);

        mvc.perform(get("/api/cards")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(cardId))
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"));
    }

    @Test
    void getAllCards_shouldReturnForbidden_whenNoAuth() throws Exception {
        SecurityContextHolder.clearContext();

        mvc.perform(get("/api/cards"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET CARD BY ID ====================

    @Test
    void getCardById_shouldReturnCard_whenExists() throws Exception {
        CardDto cardDto = CardDto.builder()
                .id(cardId)
                .maskedNumber("**** **** **** 1234")
                .expiryDate("12/28")
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("1000.00"))
                .build();

        when(cardService.findById(cardId)).thenReturn(cardDto);

        mvc.perform(get("/api/cards/{id}", cardId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cardId))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void getCardById_shouldReturnNotFound_whenCardDoesNotExist() throws Exception {
        when(cardService.findById(999L)).thenThrow(new NotFoundException("Card not found"));

        mvc.perform(get("/api/cards/{id}", 999L))
                .andExpect(status().isNotFound());
    }

    // ==================== CREATE CARD ====================

    @Test
    void createCard_shouldReturnCreated_whenValid() throws Exception {
        CreateCardRequest request = new CreateCardRequest(
                "1234567890123456",   // cardNumber
                "12/28",              // expiryDate
                1L,                   // userId
                new BigDecimal("500.00")  // balance
        );

        CardDto createdCard = CardDto.builder()
                .id(10L)
                .maskedNumber("**** **** **** 3456")
                .expiryDate("12/28")
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("500.00"))
                .build();

        when(cardService.create(any(CreateCardRequest.class))).thenReturn(createdCard);

        mvc.perform(post("/api/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/cards/10"))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.balance").value(500.00));
    }

    @Test
    void createCard_shouldReturnBadRequest_whenCardNumberIsBlank() throws Exception {
        CreateCardRequest request = new CreateCardRequest(
                "", "12/28", 1L, new BigDecimal("500.00")
        );

        mvc.perform(post("/api/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(cardService, never()).create(any());
    }

    @Test
    void createCard_shouldReturnBadRequest_whenCardNumberHasWrongLength() throws Exception {
        CreateCardRequest request = new CreateCardRequest(
                "1234", "12/28", 1L, new BigDecimal("500.00")
        );

        mvc.perform(post("/api/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(cardService, never()).create(any());
    }

    @Test
    void createCard_shouldReturnBadRequest_whenExpiryDateIsInvalid() throws Exception {
        CreateCardRequest request = new CreateCardRequest(
                "1234567890123456", "13/28", 1L, new BigDecimal("500.00")
        );

        mvc.perform(post("/api/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(cardService, never()).create(any());
    }

    @Test
    void createCard_shouldReturnBadRequest_whenUserIdIsNull() throws Exception {
        CreateCardRequest request = new CreateCardRequest(
                "1234567890123456", "12/28", null, new BigDecimal("500.00")
        );

        mvc.perform(post("/api/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(cardService, never()).create(any());
    }

    @Test
    void createCard_shouldReturnBadRequest_whenBalanceIsNegative() throws Exception {
        CreateCardRequest request = new CreateCardRequest(
                "1234567890123456", "12/28", 1L, new BigDecimal("-100.00")
        );

        mvc.perform(post("/api/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(cardService, never()).create(any());
    }

    @Test
    void createCard_shouldReturnBadRequest_whenBalanceIsNull() throws Exception {
        CreateCardRequest request = new CreateCardRequest(
                "1234567890123456", "12/28", 1L, null
        );

        mvc.perform(post("/api/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(cardService, never()).create(any());
    }

    @Test
    void createCard_shouldReturnConflict_whenCardNumberAlreadyExists() throws Exception {
        CreateCardRequest request = new CreateCardRequest(
                "1234567890123456", "12/28", 1L, new BigDecimal("500.00")
        );

        when(cardService.create(any(CreateCardRequest.class)))
                .thenThrow(new AlreadyExistsException("Card number already exists"));

        mvc.perform(post("/api/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isConflict());

        verify(cardService, times(1)).create(any(CreateCardRequest.class));
    }

    // ==================== BLOCK CARD ====================

    @Test
    void blockCard_shouldReturnNoContent_whenSuccess() throws Exception {
        doNothing().when(cardService).blockCard(cardId);

        mvc.perform(put("/api/cards/{id}/block", cardId))
                .andExpect(status().isNoContent());

        verify(cardService, times(1)).blockCard(cardId);
    }

    @Test
    void blockCard_shouldReturnNotFound_whenCardDoesNotExist() throws Exception {
        doThrow(new NotFoundException("Card not found")).when(cardService).blockCard(999L);

        mvc.perform(put("/api/cards/{id}/block", 999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void blockCard_shouldReturnBadRequest_whenCardAlreadyBlocked() throws Exception {
        doThrow(new InvalidOperationException("Card already blocked"))
                .when(cardService).blockCard(cardId);

        mvc.perform(put("/api/cards/{id}/block", cardId))
                .andExpect(status().isBadRequest());
    }

    // ==================== ACTIVATE CARD ====================

    @Test
    void activateCard_shouldReturnNoContent_whenSuccess() throws Exception {
        doNothing().when(cardService).activate(cardId);

        mvc.perform(put("/api/cards/{id}/activate", cardId))
                .andExpect(status().isNoContent());

        verify(cardService, times(1)).activate(cardId);
    }

    @Test
    void activateCard_shouldReturnNotFound_whenCardDoesNotExist() throws Exception {
        doThrow(new NotFoundException("Card not found")).when(cardService).activate(999L);

        mvc.perform(put("/api/cards/{id}/activate", 999L))
                .andExpect(status().isNotFound());
    }

    // ==================== DELETE CARD ====================

    @Test
    void deleteCard_shouldReturnNoContent_whenSuccess() throws Exception {
        doNothing().when(cardService).delete(cardId);

        mvc.perform(delete("/api/cards/{id}", cardId))
                .andExpect(status().isNoContent());

        verify(cardService, times(1)).delete(cardId);
    }

    @Test
    void deleteCard_shouldReturnNotFound_whenCardDoesNotExist() throws Exception {
        doThrow(new NotFoundException("Card not found")).when(cardService).delete(999L);

        mvc.perform(delete("/api/cards/{id}", 999L))
                .andExpect(status().isNotFound());
    }
}