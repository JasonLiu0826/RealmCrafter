package com.realmcrafter.infrastructure.persistence.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 章节表 DO。
 * 归属故事，含正文 content 与分支数据 branches_data(JSON)。
 */
@Getter
@Setter
@Entity
@Table(name = "chapter", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "story_id", "chapter_index" })
})
public class ChapterDO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 外键：所属故事 */
    @Column(name = "story_id", nullable = false, length = 32)
    private String storyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "story_id", insertable = false, updatable = false)
    private StoryDO story;

    @Column(name = "chapter_index", nullable = false)
    private Integer chapterIndex;

    @Column(length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "branches_data", nullable = false, columnDefinition = "json")
    private String branchesData;

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        if (createTime == null) createTime = LocalDateTime.now();
    }
}
