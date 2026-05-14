package com.example.bankcards.controller.card;

import com.example.bankcards.dto.card.CardDto;
import com.example.bankcards.dto.card.CreateCardRequest;
import com.example.bankcards.service.card.CardAdminService;
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
import org.springframework.web.bind.annotation.*;

/**
 * REST-контроллер для управления картами от имени администратора.
 *
 * <p><b>Доступ:</b> только пользователи с ролью {@code ADMIN}.</p>
 *
 * <p>Предоставляет полный CRUD над картами всех пользователей:</p>
 * <ul>
 *   <li>Просмотр всех карт с пагинацией</li>
 *   <li>Просмотр карты по ID</li>
 *   <li>Создание карты для любого пользователя</li>
 *   <li>Блокировка, активация и удаление карт</li>
 * </ul>
 *
 * @see CardAdminService
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/cards")
@RequiredArgsConstructor
@Tag(name = "Cards (Admin)", description = "Администрирование карт")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class CardAdminController {
    private final CardAdminService cardService;

    /**
     * Возвращает список всех карт в системе с пагинацией.
     *
     * <p>Сортировка по умолчанию: по дате создания, от новых к старым.</p>
     *
     * @param pageable параметры пагинации (размер страницы, сортировка)
     * @return {@code 200 OK} со страницей {@link CardDto}
     */
    @GetMapping
    @Operation(summary = "Все карты")
    public ResponseEntity<Page<CardDto>> getAllCards(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        log.info("ADMIN: Запрос всех карт от администратора: page={}, size={}",
                pageable.getPageNumber(), pageable.getPageSize()
        );
        return ResponseEntity.ok(cardService.findAll(pageable));
    }

    /**
     * Возвращает карту по её идентификатору.
     *
     * @param id идентификатор карты
     * @return {@code 200 OK} с {@link CardDto}
     * @throws com.example.bankcards.exception.NotFoundException c {@code 404 NOT FOUND} если карта не найдена
     */
    @GetMapping("/{id}")
    @Operation(summary = "Карта по ID (админ)")
    public ResponseEntity<CardDto> getCardById(@PathVariable Long id) {
        log.info("ADMIN: Запрос карты по ID от администратора: cardId={}", id);
        return ResponseEntity.ok(cardService.findById(id));
    }

    /**
     * Создаёт новую карту для указанного пользователя.
     *
     * <p>Номер карты должен содержать ровно 16 цифр.
     * Срок действия — в формате {@code ММ/ГГ}.</p>
     *
     * @param request DTO с данными карты (номер, срок, владелец, баланс)
     * @return {@code 201 Created} с заголовком {@code Location} и созданной картой
     * @throws com.example.bankcards.exception.NotFoundException с {@code 404 NOT FOUND} если пользователь не найден
     * @throws com.example.bankcards.exception.AccessDeniedException с {@code 403 FORBIDDEN} если пользователь ADMIN
     * @throws com.example.bankcards.exception.AlreadyExistsException с {@code 409 CONFLICT} если карта с таким номером уже существует
     * @throws com.example.bankcards.exception.InvalidOperationException с {@code 400 BAD REQUEST} если срок действия карты в прошлом или превышает 5 лет в будущем
     */
    @PostMapping
    @Operation(summary = "Создать карту")
    public ResponseEntity<CardDto> createCard(@Valid @RequestBody CreateCardRequest request) {
        log.info("Запрос на создание карты администратором: userId={}, cardNumber={}, balance={}",
                request.userId(), request.cardNumber(), request.balance()
        );
        CardDto created = cardService.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header("Location", "/api/cards/" + created.getId())
                .body(created);
    }

    /**
     * Блокирует карту по её идентификатору.
     *
     * <p>Заблокированная карта не может использоваться для транзакций.
     * Статус меняется на {@code BLOCKED}.</p>
     *
     * @param id идентификатор карты
     * @return {@code 204 No Content} при успешной блокировке
     * @throws com.example.bankcards.exception.NotFoundException с {@code 404 NOT FOUND} если карта не найдена
     * @throws com.example.bankcards.exception.InvalidOperationException с {@code 400 BAD REQUEST} если карта уже заблокирована
     */
    @PutMapping("/{id}/block")
    @Operation(summary = "Заблокировать карту")
    public ResponseEntity<Void> blockCard(@PathVariable Long id) {
        log.info("Запрос на блокировку карты администратором: cardId={}", id);

        cardService.blockCard(id);

        return ResponseEntity.noContent().build();
    }

    /**
     * Активирует ранее заблокированную карту.
     *
     * <p>Статус меняется на {@code ACTIVE}.</p>
     *
     * @param id идентификатор карты
     * @return {@code 204 No Content} при успешной активации
     * @throws com.example.bankcards.exception.NotFoundException с {@code 404 NOT FOUND} если карта не найдена
     * @throws com.example.bankcards.exception.AccessDeniedException с {@code 403 FORBIDDEN} если карта уже активна
     */
    @PutMapping("/{id}/activate")
    @Operation(summary = "Активировать карту")
    public ResponseEntity<Void> activateCard(@PathVariable Long id) {
        log.info("Запрос на активацию карты администратором: cardId={}", id);

        cardService.activate(id);

        return ResponseEntity.noContent().build();
    }

    /**
     * Удаляет карту из системы.
     *
     * <p><b>Внимание:</b> операция необратима. Все связанные транзакции также будут удалены
     * (каскадное удаление).</p>
     *
     * @param id идентификатор карты
     * @return {@code 204 No Content} при успешном удалении
     * @throws com.example.bankcards.exception.NotFoundException с {@code 404 NOT FOUND} если карта не найдена
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить карту")
    public ResponseEntity<Void> deleteCard(@PathVariable Long id) {
        log.info("Запрос на удаление карты администратором: cardId={}", id);

        cardService.delete(id);

        return ResponseEntity.noContent().build();
    }
}
