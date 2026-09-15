package com.yeniden.ecocoin.service;

import com.yeniden.ecocoin.dto.CoinEntryDto;
import com.yeniden.ecocoin.dto.GrantCoinsRequest;
import com.yeniden.ecocoin.dto.WalletDto;
import com.yeniden.ecocoin.dto.CoinGrantResult;
import com.yeniden.common.event.HandoverConfirmedEvent;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * EcoCoin Service İş Mantığı Arayüzü (04-ecocoin-rules.md).
 */
public interface EcoCoinService {
    CoinGrantResult processHandover(HandoverConfirmedEvent event);
    CoinGrantResult grantCoins(GrantCoinsRequest request);
    WalletDto getWallet(UUID userId);
    Page<CoinEntryDto> getWalletEntries(UUID userId, Pageable pageable);
}
