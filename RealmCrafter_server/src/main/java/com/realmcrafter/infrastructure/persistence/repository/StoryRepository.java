package com.realmcrafter.infrastructure.persistence.repository;

import com.realmcrafter.infrastructure.persistence.entity.StoryDO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoryRepository extends JpaRepository<StoryDO, String> {

    Page<StoryDO> findByUserIdAndStatus(Long userId, StoryDO.Status status, Pageable pageable);

    List<StoryDO> findBySettingPackIdAndStatus(String settingPackId, StoryDO.Status status);

    Page<StoryDO> findByUserIdAndStatusAndTitleContainingIgnoreCase(Long userId,
                                                                    StoryDO.Status status,
                                                                    String keyword,
                                                                    Pageable pageable);
}
