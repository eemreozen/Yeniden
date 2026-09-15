package com.yeniden.identity.auth;

import com.yeniden.identity.domain.UserStatus;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthSessionRepository extends JpaRepository<AuthSession, UUID> {
    /** Caller MUST hold the user's write lock. */
    @Modifying(flushAutomatically = true)
    @Query("update AuthSession s set s.revokedAt = :now where s.userId = :userId and s.revokedAt is null")
    int revokeAll(@Param("userId") UUID userId, @Param("now") Instant now);

    @Query("""
            select count(s) > 0 from AuthSession s, User u
            where s.id = :sessionId and s.userId = :userId and u.id = s.userId
              and s.revokedAt is null and s.expiresAt > :now
              and u.status = :status and u.phoneVerifiedAt is not null
            """)
    boolean existsActive(@Param("userId") UUID userId, @Param("sessionId") UUID sessionId,
                         @Param("now") Instant now, @Param("status") UserStatus status);
}
