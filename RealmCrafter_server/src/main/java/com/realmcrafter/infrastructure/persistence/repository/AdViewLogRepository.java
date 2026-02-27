package com.realmcrafter.infrastructure.persistence.repository;

import com.realmcrafter.infrastructure.persistence.entity.AdViewLogDO;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdViewLogRepository extends JpaRepository<AdViewLogDO, Long> {
}

