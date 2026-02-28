package com.realmcrafter.infrastructure.persistence.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 多态评论实体：支持对段落(PARAGRAPH)、选项(OPTION)、评论(COMMENT)的锚点评论与楼中楼。
 * targetType + targetRef 构成锚点；rootCommentId 用于拉取整楼回复。
 */
@Getter
@Setter
@Entity
@Table(name = "comment", indexes = {
        @Index(name = "idx_comment_story_chapter_anchor", columnList = "story_id, chapter_id, target_type, target_ref"),
        @Index(name = "idx_comment_root", columnList = "root_comment_id"),
        @Index(name = "idx_comment_create_time", columnList = "create_time")
})
public class CommentDO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "story_id", nullable = false, length = 32)
    private String storyId;

    @Column(name = "chapter_id", nullable = false)
    private Long chapterId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "likes_count", nullable = false)
    private Integer likesCount = 0;

    @Column(name = "reply_count", nullable = false)
    private Integer replyCount = 0;

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private Status status = Status.NORMAL;

    /** 锚点类型：段落、选项、评论 */
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 24)
    private TargetType targetType;

    /**
     * 锚点引用：PARAGRAPH 存段落索引如 "2"；OPTION 存选项标识如 "0" 或 "option_0"；COMMENT 存父评论 id 字符串。
     */
    @Column(name = "target_ref", nullable = false, length = 64)
    private String targetRef;

    /** 顶级评论 id，用于拉取楼中楼；顶级评论为空 */
    @Column(name = "root_comment_id")
    private Long rootCommentId;

    /** 直接父评论 id，回复时使用；顶级评论为空 */
    @Column(name = "parent_comment_id")
    private Long parentCommentId;

    @PrePersist
    protected void onCreate() {
        if (createTime == null) createTime = LocalDateTime.now();
    }

    public enum Status {
        NORMAL,
        DELETED
    }

    public enum TargetType {
        /** AI 生成段落 */
        PARAGRAPH,
        /** 创作者分支选项 */
        OPTION,
        /** 对某条评论的回复 */
        COMMENT
    }
}
