package com.yeniden.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Kullanıcı kayıt veya giriş isteği için istemciden (Frontend/Mobil) gelen veri transfer nesnesi (DTO).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    private String phone;
    private String displayName;
    private String email;
}
