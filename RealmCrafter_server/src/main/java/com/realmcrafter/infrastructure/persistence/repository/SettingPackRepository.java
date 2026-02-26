package com.realmcrafter.infrastructure.persistence.repository;

import com.realmcrafter.infrastructure.persistence.entity.SettingPackDO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SettingPackRepository extends JpaRepository<SettingPackDO, String> {

    List<SettingPackDO> findByUserIdOrderByUpdateTimeDesc(Long userId);
}
