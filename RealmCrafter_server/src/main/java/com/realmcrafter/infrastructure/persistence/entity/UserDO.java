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

    @Column(nullable = false)
    private Integer level = 1;

    @Column(name = "token_balance", nullable = false)
    private Long tokenBalance = 100_000L;

    @Column(name = "crystal_balance", nullable = false, precision = 10, scale = 2)
    private BigDecimal crystalBalance = BigDecimal.ZERO;

    @Column(name = "vip_expire_time")
    private LocalDateTime vipExpireTime;

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
