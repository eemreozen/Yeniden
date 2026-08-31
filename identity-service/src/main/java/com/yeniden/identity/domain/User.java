package com.yeniden.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * PostgreSQL veritabanındaki identity.users tablosunun Java tarafındaki JPA Entity karşılığı.
 */
@Entity
@Table(name = "users", schema = "identity")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "phone_e164", unique = true, nullable = false, length = 20)
    private String phone;

    @Column(name = "phone_verified_at")
    private Instant phoneVerifiedAt;

    @Column(name = "email")
    private String email;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "avatar_key")
    private String avatarKey;

    @Column(name = "neighborhood_id")
    private UUID neighborhoodId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status;

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", schema = "identity", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 32)
    private Set<UserRole> roles = new HashSet<>(Set.of(UserRole.USER));

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) this.createdAt = Instant.now();
        if (this.updatedAt == null) this.updatedAt = this.createdAt;
        if (this.status == null) {
            this.status = UserStatus.ACTIVE;
        }
    }

}
