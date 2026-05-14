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

/**
 * REST-контроллер для переводов между картами пользователя.
 *
 * <p><b>Доступ:</b> только пользователи с ролью {@code USER}.</p>
 *
 * <p>Пользователь может:</p>
 * <ul>
 *   <li>Переводить средства между своими картами</li>
 *   <li>Просматривать историю переводов по конкретной карте</li>
 * </ul>
 *
 * <p>Все операции проверяют, что карты принадлежат текущему пользователю.</p>
 *
 * @see TransactionUserService
 */
@Slf4j
@RestController
@RequestMapping("/api/user/transfers")
@RequiredArgsConstructor
@Tag(name = "Transfers (User)", description = "Переводы для пользователей")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('USER')")
public class TransactionUserController {
    private final TransactionUserService transactionService;

    /**
     * Выполняет перевод средств между двумя картами пользователя.
     *
     * <p>Обе карты (отправителя и получателя) должны принадлежать текущему пользователю.
     * Сумма перевода должна быть положительной и не превышать баланс карты отправителя.</p>
     *
     * <p>При успешном переводе создаётся транзакция со статусом {@code SUCCESS},
     * балансы карт обновляются атомарно.</p>
     *
     * @param request     DTO с ID карт и суммой перевода
     * @param userDetails данные текущего пользователя
     * @return {@code 201 Created} с данными созданной транзакции
     * @throws com.example.bankcards.exception.NotFoundException с {@code 404 NOT FOUND} если одна из карт не найдена
     * @throws com.example.bankcards.exception.AccessDeniedException с {@code 403 FORBIDDEN} если карта не принадлежит пользователю
     * @throws com.example.bankcards.exception.InsufficientFundsException с {@code 400 BAD REQUEST} если недостаточно средств
     * @throws com.example.bankcards.exception.InvalidOperationException с {@code 400 BAD REQUEST} если
     * <ul> <li>указана одна и та же карта</li> <li>карта неактивна или заблокирована</li> </ul>
     */
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

    /**
     * Возвращает историю транзакций по конкретной карте с пагинацией.
     *
     * <p>Включает как входящие, так и исходящие переводы.
     * Сортировка по умолчанию: от новых к старым.</p>
     *
     * @param cardId      идентификатор карты
     * @param pageable    параметры пагинации (по умолчанию 10 на странице)
     * @param userDetails данные текущего пользователя
     * @return {@code 200 OK} со страницей транзакций
     * @throws com.example.bankcards.exception.NotFoundException если карта не найдена
     * @throws com.example.bankcards.exception.AccessDeniedException если карта не принадлежит пользователю
     */
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
