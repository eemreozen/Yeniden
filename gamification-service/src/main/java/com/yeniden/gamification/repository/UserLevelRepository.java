package com.yeniden.gamification.repository;

import com.yeniden.gamification.domain.UserLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * UserLevel Entity'si için veritabanı erişim arayüzü.
 */
@Repository
public interface UserLevelRepository extends JpaRepository<UserLevel, UUID> {
}
