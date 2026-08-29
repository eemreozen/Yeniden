package com.yeniden.ecocoin.repository;

import com.yeniden.ecocoin.domain.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Wallet Entity'si için veritabanı erişim arayüzü.
 */
@Repository
public interface WalletRepository extends JpaRepository<Wallet, UUID> {
}
