package com.realmcrafter.infrastructure.persistence.repository;

import com.realmcrafter.infrastructure.persistence.entity.UserDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserDO, Long> {

    Optional<UserDO> findByUsername(String username);

    boolean existsByUsername(String username);

    Optional<UserDO> findByPhone(String phone);

    Optional<UserDO> findByWechatOpenId(String wechatOpenId);

    Optional<UserDO> findByAppleId(String appleId);

    /**
     * 原子扣减用户 Token 余额：仅当余额 ≥ amount 时更新，返回影响行数（0 或 1）。
     * 用于免费用户章节生成前扣费，避免并发超扣。
     */
    @Modifying
    @Query("UPDATE UserDO u SET u.tokenBalance = u.tokenBalance - :amount WHERE u.id = :userId AND u.tokenBalance >= :amount")
    int deductTokenBalance(@Param("userId") Long userId, @Param("amount") long amount);
}
