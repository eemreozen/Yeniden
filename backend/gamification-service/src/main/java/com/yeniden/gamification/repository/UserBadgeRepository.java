package com.yeniden.gamification.repository;

import com.yeniden.gamification.domain.UserBadge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * UserBadge Entity'si için veritabanı erişim arayüzü.
 */
@Repository
public interface UserBadgeRepository extends JpaRepository<UserBadge, UUID> {
    List<UserBadge> findByUserId(UUID userId);
    Optional<UserBadge> findByUserIdAndBadgeCode(UUID userId, String badgeCode);
    Optional<UserBadge> findByUserIdAndBadgeId(UUID userId, UUID badgeId);
}
