package com.realmcrafter.infrastructure.persistence.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 故事表 DO。
 * 关联用户与设定集，含 version_id 与 device_hash 用于多端同步。
 */
@Getter
@Setter
@Entity
@Table(name = "story")
public class StoryDO {

    @Id
    @Column(length = 32, nullable = false)
    private String id;

    /** 外键：所属用户 */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private UserDO user;

    /** 外键：使用的设定集 */
    @Column(name = "setting_pack_id", nullable = false, length = 32)
    private String settingPackId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "setting_pack_id", insertable = false, updatable = false)
    private SettingPackDO settingPack;

    /** 版权血统：来源故事 id */
    @Column(name = "source_story_id", length = 32)
    private String sourceStoryId;

    @Column(nullable = false, length = 128)
    private String title;

    @Column(length = 512)
    private String cover;

    @Column(length = 512)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    @Column(name = "allow_download", nullable = false)
    private Boolean allowDownload = false;

    @Column(name = "clone_includes_settings", nullable = false)
    private Boolean cloneIncludesSettings = false;

    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status = Status.NORMAL;

    /** 乐观锁版本号，用于多端同步防覆盖 */
    @Version
    @Column(name = "version_id", nullable = false)
    private Long versionId = 1L;

    @Column(name = "device_hash", length = 64)
    private String deviceHash;

    /**
     * 文本指纹（SimHash），用于检测洗稿与抄袭。
     */
    @Column(name = "simhash", length = 64)
    private String simhash;

    /**
     * 最近生成的章节序号，用于首页显示进度。
     */
    @Column(name = "last_chapter_index")
    private Integer lastChapterIndex = 0;

    /**
     * 最近阅读时间，用于首页列表“最近阅读”展示。
     */
    @Column(name = "last_read_time")
    private LocalDateTime lastReadTime;

    /**
     * 故事标签，逗号分隔或 JSON 存储，用于模糊搜索与展示。
     */
    @Column(name = "tags", length = 1024)
    private String tags;

    @Column(name = "likes_count", nullable = false)
    private Integer likesCount = 0;

    @Column(name = "fork_count", nullable = false)
    private Integer forkCount = 0;

    @Column(name = "favorite_count", nullable = false)
    private Integer favoriteCount = 0;

    /** 流量权重（定时任务写库）：(likesCount + forkCount*2) * 创作者等级系数，用于广场 TRAFFIC 排序 */
    @Column(name = "traffic_weight")
    private Double trafficWeight;

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

    public enum Status {
        NORMAL, DELETED
    }
}
