package com.realmcrafter.infrastructure.persistence.repository;

import com.realmcrafter.infrastructure.persistence.entity.UserExpLogDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

/**
 * 用户经验流水仓储，支持按用户+日期+行为类型统计（每日防刷）。
 */
public interface UserExpLogRepository extends JpaRepository<UserExpLogDO, Long> {

    @Query("SELECT COALESCE(SUM(e.expGained), 0) FROM UserExpLogDO e WHERE e.userId = :userId AND e.createDate = :createDate AND e.actionType = :actionType")
    long sumExpGainedByUserIdAndCreateDateAndActionType(
            @Param("userId") Long userId,
            @Param("createDate") LocalDate createDate,
            @Param("actionType") String actionType);

    @Query("SELECT COUNT(e) FROM UserExpLogDO e WHERE e.userId = :userId AND e.createDate = :createDate AND e.actionType = :actionType")
    long countByUserIdAndCreateDateAndActionType(
            @Param("userId") Long userId,
            @Param("createDate") LocalDate createDate,
            @Param("actionType") String actionType);
}
