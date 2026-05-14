package com.example.bankcards.repository;

import com.example.bankcards.dto.card.CardDto;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.enums.CardStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с сущностью {@link Card}.
 *
 * <p>Содержит методы для получения карт с проекцией в {@link CardDto}
 * (без загрузки всей сущности целиком), а также для проверки существования карт.</p>
 *
 * <p>Особенности запросов:</p>
 * <ul>
 *   <li>Методы с {@code CardDto} используют JPQL-конструктор для выборки только нужных полей</li>
 *   <li>{@link #findByIdWithUser(Long)} делает JOIN FETCH для eager-загрузки владельца</li>
 * </ul>
 */
public interface CardRepository extends JpaRepository<Card, Long> {
    /**
     * Находит карту по ID и возвращает DTO (без загрузки всей сущности).
     *
     * <p>Использует JPQL-конструктор для создания {@link CardDto} напрямую из запроса.
     * Номер карты в DTO будет замаскирован автоматически.</p>
     *
     * @param id идентификатор карты
     * @return {@link Optional} с DTO карты или {@code Optional.empty()} если карта не найдена
     */
    @Query("""
                SELECT new com.example.bankcards.dto.card.CardDto(
                    c.id,
                    c.cardNumber,
                    c.user.id,
                    c.expiryDate,
                    c.status,
                    c.balance
                )
                FROM Card c
                WHERE c.id = :id
            """)
    Optional<CardDto> findCardDtoById(@PathVariable Long id);

    /**
     * Возвращает все карты пользователя с пагинацией.
     *
     * <p>Фильтрует только по ID пользователя, без учёта статуса.
     * Для фильтрации по статусу используйте {@link #findAllCardDtoByUserIdAndStatus}.</p>
     *
     * @param userId   идентификатор пользователя-владельца
     * @param pageable параметры пагинации и сортировки
     * @return страница с DTO карт пользователя
     */
    @Query("""
                SELECT new com.example.bankcards.dto.card.CardDto(
                    c.id,
                    c.cardNumber,
                    c.user.id,
                    c.expiryDate,
                    c.status,
                    c.balance
                )
                FROM Card c
                WHERE c.user.id = :userId
            """)
    Page<CardDto> findAllCardDtoByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * Возвращает карты пользователя, отфильтрованные по статусу, с пагинацией.
     *
     * <p>Полезно для отображения только активных карт или только заблокированных.</p>
     *
     * @param userId   идентификатор пользователя-владельца
     * @param status   статус карты для фильтрации
     * @param pageable параметры пагинации и сортировки
     * @return страница с DTO карт пользователя, отфильтрованных по статусу
     */
    @Query("""
                SELECT new com.example.bankcards.dto.card.CardDto(
                    c.id,
                    c.cardNumber,
                    c.user.id,
                    c.expiryDate,
                    c.status,
                    c.balance
                )
                FROM Card c
                WHERE c.user.id = :userId
                AND c.status = :status
            """)
    Page<CardDto> findAllCardDtoByUserIdAndStatus(
            @Param("userId") Long userId,
            @Param("status") CardStatus status,
            Pageable pageable
    );

    /**
     * Возвращает все карты в системе с пагинацией (для администратора).
     *
     * <p>Без фильтрации по пользователю. Используется в админ-панели
     * для просмотра всех карт системы.</p>
     *
     * @param pageable параметры пагинации и сортировки
     * @return страница с DTO всех карт
     */
    @Query("""
                SELECT new com.example.bankcards.dto.card.CardDto(
                    c.id,
                    c.cardNumber,
                    c.user.id,
                    c.expiryDate,
                    c.status,
                    c.balance
                )
                FROM Card c
            """)
    Page<CardDto> findAllCardDto(Pageable pageable);

    /**
     * Находит карту по ID с eager-загрузкой владельца.
     *
     * <p>Использует {@code JOIN FETCH}, чтобы избежать LazyInitializationException
     * при обращении к полю {@code card.user} вне транзакции.</p>
     *
     * @param cardId идентификатор карты
     * @return {@link Optional} с сущностью карты (с загруженным владельцем)
     */
    @Query("""
            SELECT c FROM Card c
            JOIN FETCH c.user
            WHERE c.id = :cardId
            """)
    Optional<Card> findByIdWithUser(@PathVariable Long cardId);

    /**
     * Находит все карты с указанными статусами.
     *
     * <p>Используется планировщиком {@code CardExpiryScheduler} для поиска
     * активных карт с последующей проверкой срока действия.</p>
     *
     * @param statuses список статусов для фильтрации
     * @return список карт с указанными статусами
     */
    @Query("""
            SELECT c FROM Card c
            WHERE c.status IN :statuses
            """)
    List<Card> findByStatusIn(@Param("statuses") List<CardStatus> statuses);

    /**
     * Проверяет, существует ли карта с указанным номером.
     *
     * <p>Используется для валидации уникальности номера карты при создании.</p>
     *
     * @param cardNumber номер карты (16 цифр)
     * @return {@code true} если карта с таким номером уже существует
     */
    boolean existsByCardNumber(String cardNumber);

    /**
     * Проверяет, есть ли у пользователя хотя бы одна карта.
     *
     * <p>Полезно перед удалением пользователя или для проверки
     * возможности выполнения операций.</p>
     *
     * @param userId идентификатор пользователя
     * @return {@code true} если у пользователя есть карты
     */
    boolean existsByUserId(Long userId);
}
