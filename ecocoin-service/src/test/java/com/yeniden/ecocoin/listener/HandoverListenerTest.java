package com.yeniden.ecocoin.listener;

import com.yeniden.common.event.HandoverConfirmedEvent;
import com.yeniden.ecocoin.client.AccountAgeUnavailableException;
import com.yeniden.ecocoin.service.EcoCoinService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class HandoverListenerTest {
    @Mock EcoCoinService ecoCoinService;

    @Test
    void accountAgeFailureEscapesForAmqpRetry() {
        HandoverConfirmedEvent event = HandoverConfirmedEvent.builder().handoverId(UUID.randomUUID()).providerId(UUID.randomUUID()).build();
        doThrow(new AccountAgeUnavailableException(event.getProviderId())).when(ecoCoinService).processHandover(event);

        assertThrows(AccountAgeUnavailableException.class, () -> new HandoverListener(ecoCoinService).handleHandoverConfirmed(event));
    }
}
