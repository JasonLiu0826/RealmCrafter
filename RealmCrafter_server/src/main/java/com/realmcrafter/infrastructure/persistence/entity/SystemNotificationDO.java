package com.realmcrafter.infrastructure.persistence.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 系统通知：用于 @提及 等提醒。
 */
@Getter
@Setter
@Entity
@Table(name = "system_notification", indexes = {
        @Index(name = "idx_notification_user_unread", columnList = "user_id, is_read"),
        @Index(name = "idx_notification_create_time", columnList = "create_time")
})
public class SystemNotificationDO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    private NotificationType type;

    @Column(name = "title", length = 128)
    private String title;

    @Column(name = "body", columnDefinition = "text")
    private String body;

    /** 与 body 同义，部分环境表结构有 NOT NULL 的 content 列，插入时必填 */
    @Column(name = "content", length = 1000)
    private String content;

    /** 关联业务：如 comment_id、story_id 等，JSON 或 key */
    @Column(name = "ref_type", length = 32)
    private String refType;

    @Column(name = "ref_id", length = 64)
    private String refId;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        if (createTime == null) createTime = LocalDateTime.now();
        if (body == null) body = "";
        if (content == null) content = (body != null ? body : "");
    }

    public enum NotificationType {
        /** 系统类：等级提升等 */
        SYSTEM,
        /** @ 提及 */
        MENTION,
        /** 互动：点赞、收藏、评论等 */
        INTERACTION,
        /** 奖励：作品被叉（付费/免费）获得经验或水晶 */
        REWARD,
        /** 兼容旧数据：创作者等级提升 */
        LEVEL_UP
    }
}
