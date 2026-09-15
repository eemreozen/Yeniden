package com.yeniden.identity.sms;

import java.util.UUID;

/** Transport acceptance is not proof of delivery or phone ownership. */
public interface SmsSender {
    void send(String phone, String code, UUID challengeId);
}
