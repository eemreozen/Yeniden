package com.yeniden.gamification.repository;

import com.yeniden.gamification.domain.QuestRewardOutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuestRewardOutboxEventRepository extends JpaRepository<QuestRewardOutboxEvent, UUID> {
    List<QuestRewardOutboxEvent> findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();
}
