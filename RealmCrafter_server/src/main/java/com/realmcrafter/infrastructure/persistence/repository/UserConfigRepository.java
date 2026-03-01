package com.realmcrafter.infrastructure.persistence.repository;

import com.realmcrafter.infrastructure.persistence.entity.UserConfigDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserConfigRepository extends JpaRepository<UserConfigDO, Long> {

    /**
     * 原生 SQL 插入或更新引擎配置，绕过 JPA @MapsId 导致的 detached entity 问题。
     */
    @Modifying
    @Query(value = "INSERT INTO user_config (user_id, preferred_model, chaos_level, memory_depth, update_time) " +
            "VALUES (:userId, :preferredModel, :chaosLevel, :memoryDepth, NOW()) " +
            "ON DUPLICATE KEY UPDATE " +
            "preferred_model = VALUES(preferred_model), chaos_level = VALUES(chaos_level), " +
            "memory_depth = VALUES(memory_depth), update_time = NOW()",
            nativeQuery = true)
    void upsertEngineConfig(@Param("userId") Long userId,
                           @Param("preferredModel") String preferredModel,
                           @Param("chaosLevel") Double chaosLevel,
                           @Param("memoryDepth") Integer memoryDepth);
}

