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

@Slf4j
@Component
@RequiredArgsConstructor
public class CardExpiryScheduler {

    private final CardRepository cardRepository;

    @Scheduled(cron = "0 0 1 * * *")  // каждый день в 1:00 ночи
    @Transactional
    public void updateExpiredCards() {
        log.info("Запуск проверки истёкших карт");

        LocalDate now = LocalDate.now();
        int currentMonth = now.getMonthValue();
        int currentYear = now.getYear() % 100;

        List<Card> activeCards = cardRepository.findByStatusIn(List.of(CardStatus.ACTIVE, CardStatus.PENDING_BLOCK));

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
            return false;
        }
    }
}