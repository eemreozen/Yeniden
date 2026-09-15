package com.yeniden.identity;

import com.fasterxml.jackson.databind.*;
import com.yeniden.identity.auth.*;
import com.yeniden.identity.domain.UserStatus;
import com.yeniden.identity.repository.UserRepository;
import com.yeniden.identity.sms.SmsSender;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Testcontainers
@Tag("integration")
class IdentityFlowIntegrationTest {
    @Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    @Container static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);
    static final AtomicInteger numbers = new AtomicInteger(1000000);
    static final String NS = "identity-test-" + UUID.randomUUID();

    @DynamicPropertySource static void config(DynamicPropertyRegistry p) {
        p.add("spring.datasource.url", postgres::getJdbcUrl);
        p.add("spring.datasource.username", postgres::getUsername);
        p.add("spring.datasource.password", postgres::getPassword);
        p.add("spring.data.redis.host", redis::getHost);
        p.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        p.add("identity.otp.secret", () -> "integration-only-32-byte-secret-not-for-deployment");
        p.add("identity.otp.namespace", () -> NS);
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @SpyBean AuthSessionService sessions;
    @Autowired AuthUserLockRepository locks;
    @Autowired UserRepository users;
    @Autowired JwtDecoder decoder;
    @Autowired PlatformTransactionManager txManager;
    @MockBean SmsSender sender;
    final Map<UUID, String> delivered = new ConcurrentHashMap<>();
    String phone;
    String ip;

    @BeforeEach void prepare() {
        phone = "+90555" + numbers.incrementAndGet();
        ip = "192.0.2." + (numbers.get() % 200 + 1);
        doAnswer(invocation -> {
            delivered.put(invocation.getArgument(2), invocation.getArgument(1));
            return null;
        }).when(sender).send(anyString(), anyString(), any(UUID.class));
    }

    @Test void fullOtpProfileRotationLogoutFlow() throws Exception {
        JsonNode challenge = challenge();
        assertThat(challenge.has("code")).isFalse();
        assertThat(challenge.get("expiresIn").asLong()).isBetween(170L, 180L);
        JsonNode tokens = verify(challenge);
        assertThat(tokens.get("isNewUser").asBoolean()).isTrue();
        String access = tokens.get("accessToken").asText();
        var claims = decoder.decode(access);
        assertThat(claims.getClaimAsStringList("roles")).containsExactly("USER");
        assertThat(claims.getClaims()).doesNotContainKeys("phone", "email");
        mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.phone").value(phone));
        mvc.perform(get("/api/v1/users/" + claims.getSubject()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.phone").doesNotExist())
                .andExpect(jsonPath("$.data.email").doesNotExist())
                .andExpect(jsonPath("$.data.neighborhoodId").doesNotExist());
        mvc.perform(patch("/api/v1/users/me").header("Authorization", "Bearer " + access)
                        .contentType("application/json").content("{\"displayName\":\"Sunum Kullanıcısı\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.displayName").value("Sunum Kullanıcısı"));
        JsonNode rotated = postData("/api/v1/auth/refresh", Map.of("refreshToken", tokens.get("refreshToken").asText()), 200);
        assertThat(rotated.get("refreshToken").asText()).isNotEqualTo(tokens.get("refreshToken").asText());
        mvc.perform(post("/api/v1/auth/logout").contentType("application/json")
                        .content(json.writeValueAsString(Map.of("refreshToken", rotated.get("refreshToken").asText()))))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + access))
                .andExpect(status().isUnauthorized());
        postData("/api/v1/auth/refresh", Map.of("refreshToken", rotated.get("refreshToken").asText()), 401);
    }

    @Test void usedRefreshRevokesAllUserSessionsAndCommitSurvivesException() throws Exception {
        JsonNode tokens = login();
        UUID id = UUID.fromString(decoder.decode(tokens.get("accessToken").asText()).getSubject());
        TokenResponse other = new TransactionTemplate(txManager).execute(s ->
                sessions.issue(users.findById(id).orElseThrow(), false));
        String first = tokens.get("refreshToken").asText();
        postData("/api/v1/auth/refresh", Map.of("refreshToken", first), 200);
        // The caller transaction rolls back, but the replay revocation must stay committed.
        assertThatThrownBy(() -> new TransactionTemplate(txManager).execute(s -> sessions.refresh(first)))
                .isInstanceOf(AuthException.class);
        postData("/api/v1/auth/refresh", Map.of("refreshToken", other.refreshToken()), 401);
        assertThat(jdbc.queryForObject("select count(*) from identity.auth_sessions where user_id=? and revoked_at is null",
                Integer.class, id)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from identity.refresh_tokens t join identity.auth_sessions s on s.id=t.session_id where s.user_id=? and t.consumed_at is not null",
                Integer.class, id)).isEqualTo(1);
    }

    @Test void concurrentRefreshHasOneWinnerAndRevokesItOnReplay() throws Exception {
        JsonNode tokens = login();
        String raw = tokens.get("refreshToken").asText();
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService workers = Executors.newFixedThreadPool(2)) {
            Callable<Object> call = () -> {
                start.await();
                try { return sessions.refresh(raw); } catch (AuthException ex) { return ex; }
            };
            Future<Object> a = workers.submit(call);
            Future<Object> b = workers.submit(call);
            start.countDown();
            List<Object> results = List.of(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS));
            assertThat(results.stream().filter(TokenResponse.class::isInstance).count()).isEqualTo(1);
            assertThat(results.stream().filter(AuthException.class::isInstance).count()).isEqualTo(1);
            TokenResponse winner = (TokenResponse) results.stream().filter(TokenResponse.class::isInstance).findFirst().orElseThrow();
            assertThatThrownBy(() -> sessions.refresh(winner.refreshToken())).isInstanceOf(AuthException.class);
        }
    }

    @Test void otpIsSingleUseAndWrongAttemptsExhaustChallenge() throws Exception {
        JsonNode c = challenge();
        UUID id = UUID.fromString(c.get("challengeId").asText());
        String wrong = delivered.get(id).equals("000000") ? "111111" : "000000";
        for (int i = 0; i < 5; i++) postData("/api/v1/auth/otp/verify",
                Map.of("phone", phone, "code", wrong, "challengeId", id), 400);
        postData("/api/v1/auth/otp/verify",
                Map.of("phone", phone, "code", delivered.get(id), "challengeId", id), 400);
        assertThat(users.findByPhone(phone)).isEmpty();
    }

    @Test void consumedOtpCannotCreateSecondSession() throws Exception {
        JsonNode c = challenge();
        verify(c);
        postData("/api/v1/auth/otp/verify", proof(c), 400);
        UUID id = users.findByPhone(phone).orElseThrow().getId();
        assertThat(jdbc.queryForObject("select count(*) from identity.auth_sessions where user_id=?", Integer.class, id)).isEqualTo(1);
    }

    @Test void resendRateLimitIgnoresForgedForwardedHeader() throws Exception {
        challenge();
        mvc.perform(post("/api/v1/auth/otp/request").with(req -> { req.setRemoteAddr(ip); return req; })
                        .header("X-Forwarded-For", "1.2.3.4").contentType("application/json")
                        .content(json.writeValueAsString(Map.of("phone", phone))))
                .andExpect(status().isTooManyRequests()).andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.code").value("rate_limited"));
    }

    @Test void providerFailureIs503AndDoesNotCreateAccount() throws Exception {
        doThrow(new RuntimeException("provider-secret")).when(sender).send(anyString(), anyString(), any(UUID.class));
        String result = mvc.perform(post("/api/v1/auth/otp/request")
                        .with(req -> { req.setRemoteAddr(ip); return req; }).contentType("application/json")
                        .content(json.writeValueAsString(Map.of("phone", phone))))
                .andExpect(status().isServiceUnavailable()).andReturn().getResponse().getContentAsString();
        assertThat(result).doesNotContain("provider-secret", phone);
        assertThat(users.findByPhone(phone)).isEmpty();
    }

    @Test void suspendedAccountRejectsAccessRefreshAndPublicProfile() throws Exception {
        JsonNode tokens = login();
        UUID id = UUID.fromString(decoder.decode(tokens.get("accessToken").asText()).getSubject());
        new TransactionTemplate(txManager).executeWithoutResult(s -> {
            var user = locks.lockById(id).orElseThrow();
            user.setStatus(UserStatus.SUSPENDED);
            users.saveAndFlush(user);
        });
        mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + tokens.get("accessToken").asText()))
                .andExpect(status().isUnauthorized());
        postData("/api/v1/auth/refresh", Map.of("refreshToken", tokens.get("refreshToken").asText()), 401);
        mvc.perform(get("/api/v1/users/" + id)).andExpect(status().isNotFound());
    }

    @Test void profileCannotEscalateRoleOrChangePhone() throws Exception {
        JsonNode tokens = login();
        for (String body : List.of("{\"roles\":[\"ADMIN\"]}", "{\"phone\":\"+905551234567\"}",
                "{\"trustScore\":100}", "{\"status\":\"ACTIVE\"}", "{\"displayName\":\"   \"}")) {
            mvc.perform(patch("/api/v1/users/me").header("Authorization", "Bearer " + tokens.get("accessToken").asText())
                            .contentType("application/json").content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test void missingTamperedCredentialsAndExpiredSessionAreDenied() throws Exception {
        mvc.perform(get("/api/v1/users/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized());
        JsonNode tokens = login();
        UUID id = UUID.fromString(decoder.decode(tokens.get("accessToken").asText()).getSubject());
        jdbc.update("update identity.auth_sessions set created_at=now()-interval '2 days', expires_at=now()-interval '1 day' where user_id=?", id);
        mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + tokens.get("accessToken").asText()))
                .andExpect(status().isUnauthorized());
    }

    @Test void legacyBypassesAreClosedAndValidationDoesNotLeakValues() throws Exception {
        mvc.perform(post("/api/v1/identity/auth/register").contentType("application/json")
                        .content("{\"phone\":\"+905551234567\"}")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/identity/otp/send").param("phone", phone)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/identity/users/phone/" + phone)).andExpect(status().isUnauthorized());
        postData("/api/v1/auth/otp/request", Map.of("phone", "garbage"), 400);
    }

    @Test void tokensAreOnlyStoredHashed() throws Exception {
        JsonNode tokens = login();
        List<String> hashes = jdbc.queryForList("select token_hash from identity.refresh_tokens", String.class);
        assertThat(hashes).isNotEmpty().allMatch(hash -> hash.matches("[a-f0-9]{64}"))
                .doesNotContain(tokens.get("refreshToken").asText());
    }

    @Test void databaseTransactionFailureDoesNotRestoreConsumedOtp() throws Exception {
        JsonNode c = challenge();
        doThrow(new org.springframework.dao.DataAccessResourceFailureException("database-secret"))
                .when(sessions).issue(any(), anyBoolean());
        postData("/api/v1/auth/otp/verify", proof(c), 503);
        assertThat(users.findByPhone(phone)).isEmpty();
        postData("/api/v1/auth/otp/verify", proof(c), 400);
    }

    private JsonNode challenge() throws Exception {
        return postData("/api/v1/auth/otp/request", Map.of("phone", phone), 202);
    }
    private JsonNode login() throws Exception { return verify(challenge()); }
    private JsonNode verify(JsonNode c) throws Exception { return postData("/api/v1/auth/otp/verify", proof(c), 200); }
    private Map<String, Object> proof(JsonNode c) {
        UUID id = UUID.fromString(c.get("challengeId").asText());
        return Map.of("phone", phone, "code", delivered.get(id), "challengeId", id);
    }
    private JsonNode postData(String uri, Object body, int status) throws Exception {
        MvcResult result = mvc.perform(post(uri).with(req -> { req.setRemoteAddr(ip); return req; })
                        .contentType("application/json").content(json.writeValueAsString(body)))
                .andExpect(status().is(status)).andReturn();
        JsonNode root = json.readTree(result.getResponse().getContentAsString());
        return root.has("data") ? root.get("data") : root;
    }
}
