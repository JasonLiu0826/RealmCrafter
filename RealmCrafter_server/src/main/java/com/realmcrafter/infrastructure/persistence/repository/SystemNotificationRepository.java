package com.realmcrafter.infrastructure.persistence.repository;

import com.realmcrafter.infrastructure.persistence.entity.SystemNotificationDO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SystemNotificationRepository extends JpaRepository<SystemNotificationDO, Long> {

    Page<SystemNotificationDO> findByUserIdOrderByCreateTimeDesc(Long userId, Pageable pageable);

    Page<SystemNotificationDO> findByUserIdAndTypeOrderByCreateTimeDesc(Long userId, SystemNotificationDO.NotificationType type, Pageable pageable);

    @Modifying
    @Query("UPDATE SystemNotificationDO n SET n.isRead = true WHERE n.id = :id AND n.userId = :userId")
    int markAsReadByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Modifying
    @Query("UPDATE SystemNotificationDO n SET n.isRead = true WHERE n.userId = :userId")
    int markAllAsReadByUserId(@Param("userId") Long userId);
}
