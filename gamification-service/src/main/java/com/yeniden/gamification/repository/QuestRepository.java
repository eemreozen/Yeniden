package com.yeniden.gamification.repository;
import com.yeniden.gamification.domain.Quest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List; import java.util.UUID;
@Repository public interface QuestRepository extends JpaRepository<Quest, UUID> { List<Quest> findByPeriod(String period); }
