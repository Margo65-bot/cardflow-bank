package com.example.bankcards.scheduler;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Планировщик для автоматической проверки истёкших карт.
 *
 * <p>Запускается ежедневно в 01:00 ночи.
 * Находит все активные карты и карты с запросом на блокировку,
 * проверяет их срок действия и помечает просроченные как {@code EXPIRED}.</p>
 *
 * <p>Логика проверки:</p>
 * <ul>
 *   <li>Если год срока действия меньше текущего — карта просрочена</li>
 *   <li>Если год совпадает, но месяц меньше текущего — карта просрочена</li>
 * </ul>
 *
 * <p>Формат срока действия: {@code ММ/ГГ} (например, {@code 12/28}).</p>
 *
 * @see Card
 * @see CardStatus
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CardExpiryScheduler {
    private final CardRepository cardRepository;

    /**
     * Проверяет все активные карты и помечает просроченные.
     *
     * <p>Выполняется атомарно в одной транзакции.
     * Результаты логируются: количество найденных просроченных карт.</p>
     */
    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void updateExpiredCards() {
        log.info("Запуск проверки истёкших карт");

        LocalDate now = LocalDate.now();
        int currentMonth = now.getMonthValue();
        int currentYear = now.getYear() % 100;  // только две последние цифры года

        List<Card> activeCards = cardRepository.findByStatusIn(
                List.of(CardStatus.ACTIVE, CardStatus.PENDING_BLOCK)
        );

        int expiredCount = 0;
        for (Card card : activeCards) {
            if (isCardExpired(card.getExpiryDate(), currentMonth, currentYear)) {
                card.setStatus(CardStatus.EXPIRED);
                expiredCount++;
            }
        }

        cardRepository.saveAll(activeCards);
        log.info("Обновлены истёкшие карты: {}", expiredCount);
    }

    /**
     * Определяет, истёк ли срок действия карты.
     *
     * <p>Сравнивает месяц и год из срока действия с текущими.
     * Формат даты на входе: {@code "ММ/ГГ"}.</p>
     *
     * @param expiryDate   срок действия карты в формате {@code ММ/ГГ}
     * @param currentMonth текущий месяц (1-12)
     * @param currentYear  текущий год (две последние цифры)
     * @return {@code true} если карта просрочена, {@code false} если ещё действительна
     */
    private boolean isCardExpired(String expiryDate, int currentMonth, int currentYear) {
        try {
            String[] parts = expiryDate.split("/");
            int month = Integer.parseInt(parts[0]);
            int year = Integer.parseInt(parts[1]);

            if (year < currentYear) {
                return true;
            }
            if (year == currentYear && month < currentMonth) {
                return true;
            }
            return false;
        } catch (Exception e) {
            log.warn("Ошибка парсинга expiryDate: {}", expiryDate);
            return false;  // если дата в неверном формате — не трогаем карту
        }
    }
}