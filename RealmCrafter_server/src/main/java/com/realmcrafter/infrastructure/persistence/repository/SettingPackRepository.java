package com.realmcrafter.infrastructure.persistence.repository;

import com.realmcrafter.infrastructure.persistence.entity.SettingPackDO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SettingPackRepository extends JpaRepository<SettingPackDO, String> {

    Page<SettingPackDO> findByUserId(Long userId, Pageable pageable);

    Page<SettingPackDO> findByStatusAndIsPublic(SettingPackDO.AssetStatus status, Boolean isPublic, Pageable pageable);

    @Query("SELECT s FROM SettingPackDO s LEFT JOIN s.user u WHERE s.status = :status AND s.isPublic = true " +
            "AND (LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<SettingPackDO> findPublicSettingsByKeyword(@Param("status") SettingPackDO.AssetStatus status, @Param("keyword") String keyword, Pageable pageable);
}
