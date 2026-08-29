package com.yeniden.identity.repository;

import com.yeniden.identity.domain.TrustScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * TrustScore Entity'si için veritabanı erişim arayüzü (Repository).
 */
@Repository
public interface TrustScoreRepository extends JpaRepository<TrustScore, UUID> {
}
