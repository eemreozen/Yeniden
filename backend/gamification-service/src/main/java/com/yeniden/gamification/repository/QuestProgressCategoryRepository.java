package com.yeniden.gamification.repository;
import com.yeniden.gamification.domain.QuestProgressCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository public interface QuestProgressCategoryRepository extends JpaRepository<QuestProgressCategory, QuestProgressCategory.Id> {}
