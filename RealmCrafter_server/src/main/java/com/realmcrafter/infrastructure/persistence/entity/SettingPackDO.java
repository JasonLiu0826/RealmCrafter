package com.realmcrafter.infrastructure.persistence.entity;

import com.realmcrafter.domain.asset.dto.SettingContentDTO;
import com.realmcrafter.infrastructure.persistence.converter.JsonConverter;
import lombok.Getter;
import lombok.Setter;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 设定集表 DO。
 * 包含防覆盖(version_id)与版权血统(source_setting_id)。
 */
@Getter
@Setter
@Entity
@Table(name = "setting_pack")
public class SettingPackDO {

    @Id
    @Column(length = 32, nullable = false)
    private String id;

    /** 外键：所属用户 */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private UserDO user;

    /** 版权血统：来源设定集 id */
    @Column(name = "source_setting_id", length = 32)
    private String sourceSettingId;

    @Column(nullable = false, length = 128)
    private String title;

    @Column(length = 512)
    private String cover;

    @Column(length = 512)
    private String description;

    @Convert(converter = JsonConverter.class)
    @Column(nullable = false, columnDefinition = "json")
    private SettingContentDTO content;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    /** 允许克隆下载：false 时仅支持云端引用，禁止克隆到本地 */
    @Column(name = "allow_download", nullable = false)
    private Boolean allowDownload = true;

    /** 允许二次修改：fork 后的副本是否可调用更新接口 */
    @Column(name = "allow_modify", nullable = false)
    private Boolean allowModify = true;

    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AssetStatus status = AssetStatus.NORMAL;

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

    @Column(name = "likes_count", nullable = false)
    private Integer likesCount = 0;

    @Column(name = "fork_count", nullable = false)
    private Integer forkCount = 0;

    @Column(name = "favorite_count", nullable = false)
    private Integer favoriteCount = 0;

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

    public enum AssetStatus {
        NORMAL, PENDING_REVIEW, FROZEN_COPYRIGHT
    }
}
