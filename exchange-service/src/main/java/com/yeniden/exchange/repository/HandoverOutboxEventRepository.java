package com.yeniden.exchange.repository;

import com.yeniden.exchange.domain.HandoverOutboxEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HandoverOutboxEventRepository extends JpaRepository<HandoverOutboxEvent, UUID> {
    List<HandoverOutboxEvent> findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();
}
