package com.yeniden.gamification.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yeniden.gamification.domain.Quest;
import com.yeniden.gamification.repository.QuestRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MonthlyQuestProvisionerTest {
    @Mock QuestRepository questRepository;

    @Test
    void createsOnlyMissingCurrentMonthQuests() {
        when(questRepository.findByPeriodAndCode(any(), any())).thenReturn(Optional.empty());

        new MonthlyQuestProvisioner(questRepository).ensureCurrentMonth();

        verify(questRepository, times(3)).save(any(Quest.class));
    }
}
