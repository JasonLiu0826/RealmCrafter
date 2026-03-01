package com.realmcrafter.infrastructure.persistence.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 用户高阶 AI 参数配置表 DO。
 * 与用户表一对一，主键即 user_id。
 */
@Getter
@Setter
@Entity
@Table(name = "user_config")
public class UserConfigDO {

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private UserDO user;

    /**
     * 首选大模型名称，例如 realm_crafter_v1、deepseek-v3 等。
     */
    @Column(name = "preferred_model", length = 64)
    private String preferredModel = "realm_crafter_v1";

    /**
     * 混沌阈值（Temperature），范围 0.1 - 1.0，默认 0.70。
     */
    @Column(name = "chaos_level", nullable = false, columnDefinition = "decimal(5,2)")
    private Double chaosLevel = 0.70;

    /**
     * 记忆溯源深度（最大携带上下文 Token 数）。
     */
    @Column(name = "memory_depth", nullable = false)
    private Integer memoryDepth = 4000;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    @PrePersist
    @PreUpdate
    protected void onPersistOrUpdate() {
        if (updateTime == null) {
            updateTime = LocalDateTime.now();
        } else {
            updateTime = LocalDateTime.now();
        }
    }
}

