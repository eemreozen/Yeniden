package com.yeniden.identity.auth;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
    /** Scalar lookup only: do not cache mutable token/session state before taking the user lock. */
    @Query("select s.userId from RefreshToken t, AuthSession s where t.tokenHash = :hash and s.id = t.sessionId")
    Optional<UUID> findOwnerId(@Param("hash") String hash);
}
