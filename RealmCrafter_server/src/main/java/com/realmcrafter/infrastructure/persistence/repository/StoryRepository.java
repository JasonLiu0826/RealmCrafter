package com.realmcrafter.infrastructure.persistence.repository;

import com.realmcrafter.infrastructure.persistence.entity.StoryDO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StoryRepository extends JpaRepository<StoryDO, String> {

    Page<StoryDO> findByUserIdAndStatus(Long userId, StoryDO.Status status, Pageable pageable);

    List<StoryDO> findBySettingPackIdAndStatus(String settingPackId, StoryDO.Status status);

    Page<StoryDO> findByUserIdAndStatusAndTitleContainingIgnoreCase(Long userId,
                                                                    StoryDO.Status status,
                                                                    String keyword,
                                                                    Pageable pageable);

    Page<StoryDO> findByStatusAndIsPublic(StoryDO.Status status, Boolean isPublic, Pageable pageable);

    Page<StoryDO> findByStatusAndIsPublicOrderByTrafficWeightDescCreateTimeDesc(StoryDO.Status status, Boolean isPublic, Pageable pageable);

    @Query("SELECT s FROM StoryDO s LEFT JOIN s.user u WHERE s.status = :status AND s.isPublic = true " +
            "AND (LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<StoryDO> findPublicStoriesByKeyword(@Param("status") StoryDO.Status status, @Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT s FROM StoryDO s LEFT JOIN s.user u WHERE s.status = :status AND s.isPublic = true " +
            "AND (LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "ORDER BY s.trafficWeight DESC, s.createTime DESC")
    Page<StoryDO> findPublicStoriesByKeywordOrderByTrafficWeightDesc(@Param("status") StoryDO.Status status, @Param("keyword") String keyword, Pageable pageable);
}
