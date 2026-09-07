package com.yeniden.gamification.repository;

import com.yeniden.gamification.domain.Badge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

/**
 * Badge Entity'si için veritabanı erişim arayüzü.
 */
@Repository
public interface BadgeRepository extends JpaRepository<Badge, UUID> {
    Optional<Badge> findByCode(String code);
    List<Badge> findByActiveTrue();
}
