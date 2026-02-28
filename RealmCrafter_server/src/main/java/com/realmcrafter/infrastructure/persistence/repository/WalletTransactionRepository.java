package com.realmcrafter.infrastructure.persistence.repository;

import com.realmcrafter.infrastructure.persistence.entity.WalletTransactionDO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WalletTransactionRepository extends JpaRepository<WalletTransactionDO, Long> {

    Optional<WalletTransactionDO> findByExternalOrderId(String externalOrderId);
}
