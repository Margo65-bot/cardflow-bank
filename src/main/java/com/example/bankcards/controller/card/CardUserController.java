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

/**
 * REST-контроллер для операций пользователя со своими картами.
 *
 * <p><b>Доступ:</b> только пользователи с ролью {@code USER}.</p>
 *
 * <p>Пользователь может:</p>
 * <ul>
 *   <li>Просматривать свои карты с фильтрацией по статусу</li>
 *   <li>Запрашивать баланс конкретной карты</li>
 *   <li>Запрашивать блокировку своей карты (требует подтверждения администратора)</li>
 * </ul>
 *
 * <p>Все операции проверяют, что карта принадлежит текущему пользователю.</p>
 *
 * @see CardUserService
 */
@Slf4j
@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
@Tag(name = "Cards (User)", description = "Операции с картами для пользователей")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('USER')")
public class CardUserController {
    private final CardUserService cardService;

    /**
     * Возвращает список карт текущего пользователя с пагинацией.
     *
     * <p>Можно отфильтровать по статусу карты.</p>
     *
     * @param status      фильтр по статусу (ACTIVE, BLOCKED, EXPIRED), опционально
     * @param pageable    параметры пагинации (по умолчанию 10 на странице)
     * @param userDetails данные текущего пользователя из JWT-токена
     * @return {@code 200 OK} со страницей карт пользователя
     * @throws com.example.bankcards.exception.NotFoundException с {@code 404 NOT FOUND} если пользователь не найден
     */
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

    /**
     * Возвращает баланс конкретной карты пользователя.
     *
     * <p>Проверяет, что карта принадлежит текущему пользователю.</p>
     *
     * @param id          идентификатор карты
     * @param userDetails данные текущего пользователя
     * @return {@code 200 OK} с текущим балансом карты
     * @throws com.example.bankcards.exception.NotFoundException с {@code 404 NOT FOUND} если карта не найдена
     * @throws com.example.bankcards.exception.AccessDeniedException с {@code 403 FORBIDDEN} если карта не принадлежит пользователю
     */
    @GetMapping("/{id}/balance")
    @Operation(summary = "Баланс карты")
    public ResponseEntity<BigDecimal> getBalance(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("USER: Запрос баланса карты: cardId={}, userId={}", id, userDetails.getUserId());

        return ResponseEntity.ok(cardService.getBalance(id, userDetails.getUserId()));
    }

    /**
     * Отправляет запрос на блокировку карты.
     *
     * <p>Карта не блокируется немедленно — запрос отправляется на рассмотрение
     * администратору. Статус карты при этом не меняется.</p>
     *
     * @param id          идентификатор карты
     * @param userDetails данные текущего пользователя
     * @return {@code 202 Accepted} с подтверждением отправки запроса
     * @throws com.example.bankcards.exception.NotFoundException с {@code 404 NOT FOUND} если карта не найдена
     * @throws com.example.bankcards.exception.AccessDeniedException с {@code 403 FORBIDDEN} если карта не принадлежит пользователю
     * @throws com.example.bankcards.exception.InvalidOperationException с {@code 400 BAD REQUEST} если карта заблокирована или ждет блокировки
     */
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
