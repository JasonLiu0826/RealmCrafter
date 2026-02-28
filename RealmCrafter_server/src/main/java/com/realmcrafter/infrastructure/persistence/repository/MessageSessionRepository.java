package com.realmcrafter.infrastructure.persistence.repository;

import com.realmcrafter.infrastructure.persistence.entity.MessageSessionDO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MessageSessionRepository extends JpaRepository<MessageSessionDO, Long> {

    Page<MessageSessionDO> findByUserIdOrderByLastMessageAtDesc(Long userId, Pageable pageable);

    Optional<MessageSessionDO> findByUserIdAndPeerId(Long userId, Long peerId);
}
