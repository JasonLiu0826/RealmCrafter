package com.realmcrafter.infrastructure.persistence.repository;

import com.realmcrafter.infrastructure.persistence.entity.ShareRecordDO;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShareRecordRepository extends JpaRepository<ShareRecordDO, String> {
}
