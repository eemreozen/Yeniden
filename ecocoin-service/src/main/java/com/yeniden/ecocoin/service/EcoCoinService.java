package com.yeniden.ecocoin.service;

import com.yeniden.ecocoin.dto.CoinEntryDto;
import com.yeniden.ecocoin.dto.GrantCoinsRequest;
import com.yeniden.ecocoin.dto.WalletDto;

import java.util.List;
import java.util.UUID;

/**
 * EcoCoin Service İş Mantığı Arayüzü (04-ecocoin-rules.md).
 */
public interface EcoCoinService {
    WalletDto grantCoins(GrantCoinsRequest request);
    WalletDto getWallet(UUID userId);
    List<CoinEntryDto> getWalletEntries(UUID userId);
}
