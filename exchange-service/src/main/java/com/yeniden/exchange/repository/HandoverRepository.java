package com.yeniden.exchange.repository;

import com.yeniden.exchange.domain.Handover;
import com.yeniden.exchange.domain.HandoverStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Handover Entity'si için veritabanı erişim arayüzü.
 */
@Repository
public interface HandoverRepository extends JpaRepository<Handover, UUID> {
    Optional<Handover> findByRequestId(UUID requestId);
    List<Handover> findByProviderIdOrReceiverId(UUID providerId, UUID receiverId);
    List<Handover> findByStatus(HandoverStatus status);
}
