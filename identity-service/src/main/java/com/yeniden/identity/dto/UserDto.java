package com.yeniden.identity.dto;

import com.yeniden.identity.domain.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Kullanıcı bilgilerinin istemciye güvenle dönülmesi için kullanılan DTO nesnesi.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private UUID id;
    private String phone;
    private String email;
    private String displayName;
    private String avatarKey;
    private UUID neighborhoodId;
    private UserStatus status;
    private int trustScore;
    private LocalDateTime createdAt;
}
