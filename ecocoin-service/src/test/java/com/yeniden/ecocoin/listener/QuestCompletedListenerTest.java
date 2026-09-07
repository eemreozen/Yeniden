package com.yeniden.ecocoin.listener;

import com.yeniden.common.event.QuestCompletedEvent;
import com.yeniden.ecocoin.dto.CoinGrantReason;
import com.yeniden.ecocoin.dto.CoinGrantResult;
import com.yeniden.ecocoin.dto.WalletDto;
import com.yeniden.ecocoin.service.EcoCoinService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuestCompletedListenerTest {
    @Mock EcoCoinService ecoCoinService;

    @Test void validQuestEventUsesExistingQuestGrantContract() {
        UUID userId = UUID.randomUUID(); UUID questId = UUID.randomUUID();
        when(ecoCoinService.grantCoins(any())).thenReturn(CoinGrantResult.builder().granted(true).amount(25).reason(CoinGrantReason.GRANTED).wallet(WalletDto.builder().build()).build());
        new QuestCompletedListener(ecoCoinService).handle(QuestCompletedEvent.builder().eventId(UUID.randomUUID()).userId(userId).questId(questId).rewardCoins(25).completedAt(Instant.now()).build());
        ArgumentCaptor<com.yeniden.ecocoin.dto.GrantCoinsRequest> request = ArgumentCaptor.forClass(com.yeniden.ecocoin.dto.GrantCoinsRequest.class);
        verify(ecoCoinService).grantCoins(request.capture());
        assertEquals("QUEST_COMPLETED", request.getValue().getReason());
        assertEquals("quest:" + userId + ":" + questId, request.getValue().getIdempotencyKey());
    }

    @Test void invalidQuestEventDoesNotGrant() {
        new QuestCompletedListener(ecoCoinService).handle(QuestCompletedEvent.builder().eventId(UUID.randomUUID()).userId(UUID.randomUUID()).questId(UUID.randomUUID()).rewardCoins(0).build());
        verify(ecoCoinService, never()).grantCoins(any());
    }
}
