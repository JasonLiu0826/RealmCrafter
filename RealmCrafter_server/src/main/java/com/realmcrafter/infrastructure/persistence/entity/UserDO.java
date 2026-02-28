package com.realmcrafter.infrastructure.persistence.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户表 DO。
 * 带算力(token_balance)与虚拟币(crystal_balance)双资产。
 */
@Getter
@Setter
@Entity
@Table(name = "user")
public class UserDO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String username;

    @Column(nullable = false, length = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private UserRole role = UserRole.USER;

    @Column(length = 512)
    private String avatar;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private Gender gender = Gender.UNKNOWN;

    @Column(name = "age")
    private Integer age;

    /** 创作者等级 1~5，用于分润与定价上限 */
    @Column(nullable = false)
    private Integer level = 1;

    /** 经验值，用于等级跃迁 */
    @Column(nullable = false)
    private Long exp = 0L;

    /** 金牌签约创作者，享受更高分润与流量系数 */
    @Column(name = "is_golden_creator", nullable = false)
    private Boolean isGoldenCreator = false;

    @Column(name = "current_theme_id", length = 32)
    private String currentThemeId = "classic_white";

    @Column(name = "token_balance", nullable = false)
    private Long tokenBalance = 100_000L;

    @Column(name = "crystal_balance", nullable = false, precision = 10, scale = 2)
    private BigDecimal crystalBalance = BigDecimal.ZERO;

    @Column(name = "vip_expire_time")
    private LocalDateTime vipExpireTime;

    /**
     * 是否开启自定义 API（BYOK）模式。
     */
    @Column(name = "is_byok", nullable = false)
    private Boolean isByok = false;

    /**
     * AI 交互计数器（每生成 1 章 +1，满 10 章触发广告）。
     */
    @Column(name = "interaction_counter", nullable = false)
    private Integer interactionCounter = 0;

    /**
     * 免广告特权到期时间（如看一次视频免 1 小时）。
     */
    @Column(name = "ad_free_expire_time")
    private LocalDateTime adFreeExpireTime;

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

    public enum UserRole {
        USER, ADMIN, SUPER_ADMIN
    }

    public enum Gender {
        MALE, FEMALE, UNKNOWN
    }
}
