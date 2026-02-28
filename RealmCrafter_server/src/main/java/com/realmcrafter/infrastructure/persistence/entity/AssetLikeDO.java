package com.realmcrafter.infrastructure.persistence.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 点赞记录：幂等点赞/取消赞，用于更新 DO 的 likesCount。
 */
@Getter
@Setter
@Entity
@Table(name = "asset_like", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "user_id", "asset_type", "asset_id" })
})
public class AssetLikeDO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "asset_type", nullable = false, length = 16)
    @Enumerated(EnumType.STRING)
    private AssetType assetType;

    @Column(name = "asset_id", nullable = false, length = 32)
    private String assetId;

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        if (createTime == null) createTime = LocalDateTime.now();
    }

    public enum AssetType {
        STORY,
        SETTING,
        /** 评论点赞 */
        COMMENT
    }
}
