package com.yeniden.exchange.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Paylaşan tarafın 6 haneli kodu doğrulamak için gönderdiği istek gövdesi.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmCodeRequest {
    private UUID providerId;
    private String code;
}
