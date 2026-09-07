package com.yeniden.gamification.repository;
import com.yeniden.gamification.domain.QuestProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional; import java.util.UUID;
@Repository public interface QuestProgressRepository extends JpaRepository<QuestProgress, UUID> { Optional<QuestProgress> findByUserIdAndQuestId(UUID userId, UUID questId); }
