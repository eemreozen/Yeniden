package com.yeniden.ecocoin.controller;

import com.yeniden.common.result.ApiResponse;
import com.yeniden.ecocoin.dto.CoinEntryDto;
import com.yeniden.ecocoin.dto.WalletDto;
import com.yeniden.ecocoin.service.EcoCoinService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/v1/ecocoin")
@RequiredArgsConstructor
public class EcoCoinController {

    private final EcoCoinService ecoCoinService;

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        return ResponseEntity.ok(ApiResponse.success("EcoCoin Service is running"));
    }

    @GetMapping("/wallet/{userId}")
    public ResponseEntity<ApiResponse<WalletDto>> getWallet(@PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(ecoCoinService.getWallet(userId)));
    }

    @GetMapping("/wallet/{userId}/entries")
    public ResponseEntity<ApiResponse<Page<CoinEntryDto>>> getWalletEntries(@PathVariable UUID userId,
                                                                              @RequestParam(defaultValue = "0") int page,
                                                                              @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(ecoCoinService.getWalletEntries(userId, PageRequest.of(page, size))));
    }
}
