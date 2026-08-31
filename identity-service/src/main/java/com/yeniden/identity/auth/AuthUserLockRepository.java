package com.yeniden.identity.auth;

import com.yeniden.identity.domain.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * Shared serialization point for issue/refresh/logout and account-wide invalidation.
 * Suspend/delete/role changes must acquire this lock BEFORE reading or changing auth state.
 * Always lock user first, then session/token. Call inside a writable transaction.
 */
@Repository
public class AuthUserLockRepository {
    private final EntityManager entityManager;

    public AuthUserLockRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Optional<User> lockById(UUID id) {
        // Preserve newly verified users in the enclosing OTP transaction before refreshing them.
        entityManager.flush();
        User user = entityManager.find(User.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (user != null) {
            // A caller/OSIV persistence context can already contain an older account status.
            entityManager.refresh(user, LockModeType.PESSIMISTIC_WRITE);
        }
        return Optional.ofNullable(user);
    }
}
