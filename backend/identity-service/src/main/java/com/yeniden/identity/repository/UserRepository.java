package com.yeniden.identity.repository;

import com.yeniden.identity.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * User Entity'si için veritabanı erişim arayüzü (Repository).
 * Spring Data JPA sayesinde SQL yazmadan CRUD ve özel sorgu metotları sağlar.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Telefon numarasına göre kullanıcı arar.
     * Spring Data JPA metot isminden ("findByPhone") otomatik SQL türetir:
     * SELECT * FROM identity.users WHERE phone_e164 = ?
     */
    Optional<User> findByPhone(String phone);

    /**
     * Bu telefon numarasıyla daha önce kayıt olunmuş mu kontrol eder.
     * SQL: SELECT COUNT(*) > 0 FROM identity.users WHERE phone_e164 = ?
     */
    boolean existsByPhone(String phone);
}
