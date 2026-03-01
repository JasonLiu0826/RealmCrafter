package com.realmcrafter.infrastructure.persistence.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 私信消息：文本或站内转发卡片（FORWARD_CARD 时 content 为 JSON Payload）。
 */
@Getter
@Setter
@Entity
@Table(name = "message", indexes = {
        @Index(name = "idx_message_sender_receiver_time", columnList = "sender_id, receiver_id, create_time"),
        @Index(name = "idx_message_receiver_read", columnList = "receiver_id, is_read")
})
public class MessageDO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    @Enumerated(EnumType.STRING)
    @Column(name = "msg_type", nullable = false, length = 24)
    private MsgType msgType;

    /** 文本内容，或 FORWARD_CARD 时的 JSON Payload */
    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    /** 与 content 同义，按普通文本存储（TEXT），与 content 保持一致 */
    @Column(name = "payload", columnDefinition = "text")
    private String payload;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        if (createTime == null) createTime = LocalDateTime.now();
        if (payload == null) payload = (content != null ? content : "");
    }

    public enum MsgType {
        TEXT,
        FORWARD_CARD
    }
}
