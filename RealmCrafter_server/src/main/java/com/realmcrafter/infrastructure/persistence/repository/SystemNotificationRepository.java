package com.realmcrafter.infrastructure.persistence.repository;

import com.realmcrafter.infrastructure.persistence.entity.SystemNotificationDO;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemNotificationRepository extends JpaRepository<SystemNotificationDO, Long> {
}
