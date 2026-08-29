package com.yeniden.ecocoin.controller;

import com.yeniden.common.result.ApiResponse;
import com.yeniden.ecocoin.dto.CoinEntryDto;
import com.yeniden.ecocoin.dto.GrantCoinsRequest;
import com.yeniden.ecocoin.dto.WalletDto;
import com.yeniden.ecocoin.service.EcoCoinService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * EcoCoin Service REST API Uç Noktaları.
 */
@RestController
@RequestMapping("/api/v1/ecocoin")
@RequiredArgsConstructor
public class EcoCoinController {

    private final EcoCoinService ecoCoinService;

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        return ResponseEntity.ok(ApiResponse.success("EcoCoin Service çalışıyor!"));
    }

    @PostMapping("/grant")
    public ResponseEntity<ApiResponse<WalletDto>> grantCoins(@RequestBody GrantCoinsRequest request) {
        WalletDto dto = ecoCoinService.grantCoins(request);
        return ResponseEntity.ok(ApiResponse.success("Eco-Coin puan kazanımı işlendi", dto));
    }

    @GetMapping("/wallet/{userId}")
    public ResponseEntity<ApiResponse<WalletDto>> getWallet(@PathVariable("userId") UUID userId) {
        WalletDto dto = ecoCoinService.getWallet(userId);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @GetMapping("/wallet/{userId}/entries")
    public ResponseEntity<ApiResponse<List<CoinEntryDto>>> getWalletEntries(@PathVariable("userId") UUID userId) {
        List<CoinEntryDto> entries = ecoCoinService.getWalletEntries(userId);
        return ResponseEntity.ok(ApiResponse.success(entries));
    }
}
