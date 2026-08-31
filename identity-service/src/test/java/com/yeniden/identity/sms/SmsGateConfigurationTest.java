package com.yeniden.identity.sms;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

import static com.yeniden.identity.sms.SmsGateSenderTest.assertUnavailable;
import static org.junit.jupiter.api.Assertions.*;

class SmsGateConfigurationTest {
    @Test
    void springRegistersFailClosedPortWithSafeDefaults() {
        new ApplicationContextRunner().withUserConfiguration(SmsGateConfiguration.class).run(context -> {
            assertNull(context.getStartupFailure());
            assertEquals(1, context.getBeansOfType(SmsSender.class).size());
            SmsGateProperties properties = context.getBean(SmsGateProperties.class);
            assertFalse(properties.isEnabled());
            assertFalse(properties.isAllowPrivateHttp());
            assertTrue(properties.getAllowedRecipients().isEmpty());
            assertEquals(Duration.ofSeconds(3), properties.getConnectTimeout());
            assertEquals(Duration.ofSeconds(5), properties.getReadTimeout());
            assertUnavailable(() -> context.getBean(SmsSender.class).send("+12025550123", "012345", UUID.randomUUID()));
        });
    }

    @Test
    void bindsExactConfigurationNamesWithoutExposingCredentialsInToString() {
        new ApplicationContextRunner().withUserConfiguration(SmsGateConfiguration.class)
                .withPropertyValues("identity.sms-gate.enabled=true",
                        "identity.sms-gate.base-url=http://127.0.0.1:8080",
                        "identity.sms-gate.username=stub-user", "identity.sms-gate.password=stub-password",
                        "identity.sms-gate.allow-private-http=true",
                        "identity.sms-gate.allowed-recipients[0]=+12025550123",
                        "identity.sms-gate.connect-timeout=250ms", "identity.sms-gate.read-timeout=2s")
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    SmsGateProperties properties = context.getBean(SmsGateProperties.class);
                    assertTrue(properties.isEnabled());
                    assertTrue(properties.isAllowPrivateHttp());
                    assertEquals(Set.of("+12025550123"), properties.getAllowedRecipients());
                    assertEquals(Duration.ofMillis(250), properties.getConnectTimeout());
                    assertEquals(Duration.ofSeconds(2), properties.getReadTimeout());
                    assertFalse(properties.toString().contains("stub-user"));
                    assertFalse(properties.toString().contains("stub-password"));
                    // No send: this test only verifies bean creation/configuration binding.
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://10.0.0.1:8080", "http://172.16.0.1", "http://172.31.255.254/",
            "http://192.168.1.42", "http://127.0.0.1", "http://[::1]:8080",
            "http://[fd12:3456::1]", "http://[fc00::1]", "https://gateway.example.test",
            "https://192.168.1.42:8443/"})
    void acceptsExplicitPrivateLiteralsOrHttps(String url) {
        assertEquals("/message", SmsGateSender.messageEndpoint(url, true).getPath());
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://8.8.8.8", "http://localhost", "http://gateway.internal",
            "http://127.0.0.1.example.com", "http://2130706433", "http://127.1", "http://0177.0.0.1",
            "http://10.0.0.999", "http://172.15.0.1", "http://172.32.0.1", "http://192.169.0.1",
            "http://0.0.0.0", "http://169.254.169.254", "http://100.64.0.1",
            "http://[::]", "http://[fe80::1]", "http://[2001:4860:4860::8888]",
            "http://[::ffff:127.0.0.1]", "http://[fe80::1%25en0]",
            "ftp://127.0.0.1", "https://user:secret@gateway.example.test", "https://gateway.example.test?token=secret",
            "https://gateway.example.test/#secret", "https://gateway.example.test/prefix",
            "https://gateway.example.test/%2fmessage", "https://gateway.example.test:0",
            "https://gateway.example.test:65536", "//gateway.example.test", "not a URI", ""})
    void rejectsUnsafeOrAmbiguousUrlsWithoutEchoingThem(String url) {
        assertUnavailable(() -> SmsGateSender.messageEndpoint(url, true));
    }

    @Test
    void httpsDoesNotRequirePlaintextOptInButPrivateHttpDoes() {
        assertEquals("https://gateway.example.test/message",
                SmsGateSender.messageEndpoint("https://gateway.example.test/", false).toString());
        assertUnavailable(() -> SmsGateSender.messageEndpoint("http://192.168.1.42:8080", false));
        assertUnavailable(() -> SmsGateSender.messageEndpoint(null, true));
    }
}
