package com.realmcrafter.infrastructure.persistence.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 广告观看激励流水表 DO。
 * 用于广告引擎对账和奖励发放追踪。
 */
@Getter
@Setter
@Entity
@Table(name = "ad_view_log", indexes = {
        @Index(name = "idx_user_time", columnList = "user_id, create_time")
})
public class AdViewLogDO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private UserDO user;

    @Enumerated(EnumType.STRING)
    @Column(name = "ad_platform", nullable = false, length = 16)
    private AdPlatform adPlatform;

    @Enumerated(EnumType.STRING)
    @Column(name = "ad_type", nullable = false, length = 32)
    private AdType adType;

    /**
     * 发放的奖励（如：5000_TOKEN / 1_HOUR_AD_FREE）。
     */
    @Column(name = "reward_granted", nullable = false, length = 64)
    private String rewardGranted;

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        if (createTime == null) {
            createTime = LocalDateTime.now();
        }
    }

    public enum AdPlatform {
        ADMOB, PANGLE, UNITY
    }

    public enum AdType {
        REWARD_VIDEO, INTERSTITIAL
    }
}

