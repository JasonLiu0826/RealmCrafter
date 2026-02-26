package com.realmcrafter.infrastructure.persistence.repository;

import com.realmcrafter.infrastructure.persistence.entity.SettingPackDO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettingPackRepository extends JpaRepository<SettingPackDO, String> {

    Page<SettingPackDO> findByUserId(Long userId, Pageable pageable);
}
