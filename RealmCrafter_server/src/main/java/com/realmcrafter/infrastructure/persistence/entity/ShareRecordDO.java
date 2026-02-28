package com.realmcrafter.infrastructure.persistence.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 转发/分享记录：短链与锚点映射，用于站外深度链接解析。
 */
@Getter
@Setter
@Entity
@Table(name = "share_record", indexes = {
        @Index(name = "idx_share_create_time", columnList = "create_time")
})
public class ShareRecordDO {

    @Id
    @Column(name = "short_code", nullable = false, unique = true, length = 16)
    private String shortCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 24)
    private ShareType type;

    @Column(name = "story_id", nullable = false, length = 32)
    private String storyId;

    @Column(name = "chapter_id", nullable = false)
    private Long chapterId;

    @Column(name = "target_ref", length = 64)
    private String targetRef;

    @Column(name = "excerpt", length = 512)
    private String excerpt;

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        if (createTime == null) createTime = LocalDateTime.now();
    }

    public enum ShareType {
        PARAGRAPH,
        OPTION,
        COMMENT
    }
}
