package com.yeniden.gamification.domain;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Entity @Table(name = "user_categories", schema = "gamification") @Getter @NoArgsConstructor
public class UserCategory {
    @EmbeddedId private UserCategoryId id;
    public UserCategory(UUID userId, UUID categoryId) { id = new UserCategoryId(userId, categoryId); }
    @Embeddable @Getter @EqualsAndHashCode @NoArgsConstructor @AllArgsConstructor
    public static class UserCategoryId implements Serializable { private UUID userId; private UUID categoryId; }
}
