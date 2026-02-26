package com.realmcrafter.infrastructure.persistence.repository;

import com.realmcrafter.infrastructure.persistence.entity.StoryDO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoryRepository extends JpaRepository<StoryDO, String> {

    List<StoryDO> findByUserIdOrderByUpdateTimeDesc(Long userId);

    List<StoryDO> findBySettingPackId(String settingPackId);
}
