package com.realmcrafter.infrastructure.persistence.repository;

import com.realmcrafter.infrastructure.persistence.entity.MessageDO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<MessageDO, Long> {

    /** 两人之间的聊天记录：A 与 B 互相发送的，按时间倒序 */
    @Query("SELECT m FROM MessageDO m WHERE (m.senderId = :userId AND m.receiverId = :peerId) OR (m.senderId = :peerId AND m.receiverId = :userId) ORDER BY m.createTime DESC")
    Page<MessageDO> findChatBetween(@Param("userId") Long userId, @Param("peerId") Long peerId, Pageable pageable);
}
