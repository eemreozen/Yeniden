package com.yeniden.gamification.service;

import com.yeniden.gamification.domain.Quest;
import com.yeniden.gamification.repository.QuestRepository;
import java.time.YearMonth;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class MonthlyQuestProvisioner {
    private final QuestRepository questRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(cron = "${gamification.quest-provision.cron:0 5 0 * * *}")
    @Transactional
    public void ensureCurrentMonth() {
        String period = YearMonth.now().toString();
        ensure(period, "MONTHLY_GIVE_3", "Bu ay 3 paylaşım tamamla", "CONFIRMED_GIVE_COUNT", 3, 20);
        ensure(period, "MONTHLY_TAKE_3", "Bu ay 3 malzemeyi döngüye kazandır", "CONFIRMED_TAKE_COUNT", 3, 20);
        ensure(period, "MONTHLY_ECO_100", "Bu ay 100 Eco-Coin kazan", "TOTAL_COINS_EARNED", 100, 15);
    }

    private void ensure(String period, String code, String title, String metric, long target, long reward) {
        if (questRepository.findByPeriodAndCode(period, code).isPresent()) {
            return;
        }
        questRepository.save(Quest.builder()
                .period(period)
                .code(code)
                .title(title)
                .targetMetric(metric)
                .targetValue(target)
                .rewardCoins(reward)
                .build());
    }
}
