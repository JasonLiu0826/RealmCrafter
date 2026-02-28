package com.realmcrafter.infrastructure.persistence.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 会话摘要：用于「最近聊天列表」快速展示，每个用户-对方一条记录，存最后一条消息摘要与未读数。
 */
@Getter
@Setter
@Entity
@Table(name = "message_session", indexes = {
        @Index(name = "idx_session_user_updated", columnList = "user_id, last_message_at")
}, uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "peer_id"})
})
public class MessageSessionDO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 当前用户（会话归属视角） */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 对方用户 */
    @Column(name = "peer_id", nullable = false)
    private Long peerId;

    /** 最后一条消息 ID */
    @Column(name = "last_message_id")
    private Long lastMessageId;

    /** 最后一条消息摘要（用于列表展示） */
    @Column(name = "last_message_preview", length = 256)
    private String lastMessagePreview;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    /** 当前用户在此会话中的未读条数（对方发给我且未读） */
    @Column(name = "unread_count", nullable = false)
    private Integer unreadCount = 0;

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createTime == null) createTime = now;
        if (updateTime == null) updateTime = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
