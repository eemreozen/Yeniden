package com.yeniden.gamification.repository;
import com.yeniden.gamification.domain.UserGamificationProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;
@Repository public interface UserGamificationProfileRepository extends JpaRepository<UserGamificationProfile, UUID> {}
