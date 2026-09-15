package com.yeniden.identity.otp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeniden.common.exception.BaseException;
import com.yeniden.identity.sms.SmsSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OtpServiceTest {
    private final RedisOtpStore store = mock(RedisOtpStore.class);
    private final SmsSender sender = mock(SmsSender.class);
    private final OtpCrypto crypto = new OtpCrypto(OtpPropertiesTest.validProperties());
    private final OtpService service = new OtpService(store, sender, crypto);
    private static final String PHONE = "+905321234567";
    private static final String IP = "192.0.2.1";

    @BeforeEach
    void activateByDefault() {
        when(store.activate(anyString(), any())).thenReturn(new RedisOtpStore.Activation(178_500, 58_500));
    }

    @Test
    void reservesHashedPendingProofBeforeSendingAndActivatesOnlyAfterAcceptance() throws Exception {
        OtpChallengeResponse response = service.request("(0532) 123-45-67", IP);
        ArgumentCaptor<String> proof = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        var order = inOrder(store, sender);
        order.verify(store).reserve(eq(crypto.phoneKey(PHONE)), eq(crypto.ipKey(IP)),
                eq(response.challengeId()), proof.capture());
        order.verify(sender).send(eq(PHONE), code.capture(), eq(response.challengeId()));
        order.verify(store).activate(crypto.phoneKey(PHONE), response.challengeId());
        assertThat(code.getValue()).matches("[0-9]{6}");
        assertThat(proof.getValue()).isEqualTo(crypto.proof(PHONE, code.getValue(), response.challengeId()));
        assertThat(response.expiresIn()).isEqualTo(179);
        assertThat(response.retryAfterSeconds()).isEqualTo(59);
        assertThat(new ObjectMapper().valueToTree(response).fieldNames())
                .toIterable().containsExactlyInAnyOrder("challengeId", "expiresIn", "retryAfterSeconds");
        verifyNoMoreInteractions(sender);
    }

    @Test
    void rateLimitDoesNotSendOrActivateAndRoundsRetryUp() {
        when(store.reserve(anyString(), anyString(), any(), anyString())).thenReturn(60_001L);
        assertThatThrownBy(() -> service.request(PHONE, IP))
                .isInstanceOfSatisfying(RateLimitedException.class, e -> {
                    assertThat(e.getHttpStatus()).isEqualTo(429);
                    assertThat(e.getErrorCode()).isEqualTo("rate_limited");
                    assertThat(e.getRetryAfterSeconds()).isEqualTo(61);
                    assertThat(e.retryAfterSeconds()).isEqualTo(61);
                });
        verifyNoInteractions(sender);
        verify(store, never()).activate(anyString(), any());
    }

    @Test
    void ambiguousSendFailureInvalidatesOnlyItsChallengeAndNeverRetriesOrLeaksCause() {
        doThrow(new IllegalStateException("provider echoed " + PHONE))
                .when(sender).send(anyString(), anyString(), any());
        assertUnavailable(() -> service.request(PHONE, IP));
        ArgumentCaptor<UUID> sentId = ArgumentCaptor.forClass(UUID.class);
        verify(sender, times(1)).send(eq(PHONE), anyString(), sentId.capture());
        verify(store).invalidate(crypto.phoneKey(PHONE), sentId.getValue());
        verify(store, never()).activate(anyString(), any());
    }

    @Test
    void cleanupFailureIsAlsoSanitized() {
        doThrow(new IllegalStateException(PHONE)).when(sender).send(anyString(), anyString(), any());
        doThrow(new IllegalStateException(PHONE)).when(store).invalidate(anyString(), any());
        assertUnavailable(() -> service.request(PHONE, IP));
        verify(sender, times(1)).send(eq(PHONE), anyString(), any());
    }

    @Test
    void expiredOrReplacedReservationCannotBecomeASuccessfulResponse() {
        when(store.activate(anyString(), any())).thenReturn(new RedisOtpStore.Activation(0, 0));
        assertUnavailable(() -> service.request(PHONE, IP));
        verify(store).invalidate(eq(crypto.phoneKey(PHONE)), any());
        verify(sender, times(1)).send(eq(PHONE), anyString(), any());
    }

    @Test
    void redisFailureBeforeReserveDoesNotSend() {
        when(store.reserve(anyString(), anyString(), any(), anyString())).thenThrow(OtpErrors.unavailable());
        assertUnavailable(() -> service.request(PHONE, IP));
        verifyNoInteractions(sender);
    }

    @Test
    void redisFailureAfterSendingDoesNotRepeatSend() {
        when(store.activate(anyString(), any())).thenThrow(OtpErrors.unavailable());
        assertUnavailable(() -> service.request(PHONE, IP));
        verify(sender, times(1)).send(eq(PHONE), anyString(), any());
        verify(store).invalidate(eq(crypto.phoneKey(PHONE)), any());
    }

    @Test
    void verificationConsumesBeforeReturningNormalizedPhone() {
        UUID id = UUID.randomUUID();
        when(store.consume(crypto.phoneKey(PHONE), id, crypto.proof(PHONE, "001234", id)))
                .thenReturn(true).thenReturn(false);
        assertThat(service.verify("05321234567", "001234", id)).isEqualTo(PHONE);
        assertInvalid(() -> service.verify(PHONE, "001234", id));
        verifyNoInteractions(sender);
    }

    @Test
    void wrongAndMalformedCodesUseTheAttemptCounter() {
        UUID id = UUID.randomUUID();
        assertInvalid(() -> service.verify(PHONE, "123456", id));
        assertInvalid(() -> service.verify(PHONE, "12oops", id));
        assertInvalid(() -> service.verify(PHONE, null, id));
        verify(store).consume(crypto.phoneKey(PHONE), id, crypto.proof(PHONE, "123456", id));
        verify(store, times(2)).consume(crypto.phoneKey(PHONE), id, "invalid");
    }

    @Test
    void missingChallengeAndBadPhoneOrIpFailBeforeAnySideEffect() {
        assertInvalid(() -> service.verify(PHONE, "123456", null));
        assertInvalid(() -> service.request("not a number", IP));
        assertInvalid(() -> service.request(PHONE, null));
        assertInvalid(() -> service.request(PHONE, " "));
        verifyNoInteractions(sender, store);
    }

    private static void assertInvalid(Runnable action) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(BaseException.class, e -> {
            assertThat(e.getHttpStatus()).isEqualTo(400);
            assertThat(e.getErrorCode()).isEqualTo("invalid_otp");
        });
    }

    private static void assertUnavailable(Runnable action) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(BaseException.class, e -> {
            assertThat(e.getHttpStatus()).isEqualTo(503);
            assertThat(e.getErrorCode()).isEqualTo("service_unavailable");
            assertThat(e.getMessage()).doesNotContain(PHONE);
            assertThat(e.getCause()).isNull();
        });
    }
}
