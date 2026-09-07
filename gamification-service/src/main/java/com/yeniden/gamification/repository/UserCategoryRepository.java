package com.yeniden.gamification.repository;
import com.yeniden.gamification.domain.UserCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository public interface UserCategoryRepository extends JpaRepository<UserCategory, UserCategory.UserCategoryId> {}
