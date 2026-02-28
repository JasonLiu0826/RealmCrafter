package com.realmcrafter.infrastructure.persistence.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户经验流水表，用于每日防刷统计与行为回顾。
 * actionType 存枚举 name，与 domain/user/ExpAction 对应。
 */
@Getter
@Setter
@Entity
@Table(name = "user_exp_log", indexes = {
        @Index(name = "idx_user_exp_log_user_date", columnList = "user_id, create_date"),
        @Index(name = "idx_user_exp_log_user_date_action", columnList = "user_id, create_date, action_type")
})
public class UserExpLogDO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "action_type", nullable = false, length = 32)
    private String actionType;

    @Column(name = "exp_gained", nullable = false)
    private Long expGained;

    @Column(name = "create_date", nullable = false)
    private LocalDate createDate;

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        if (createTime == null) createTime = LocalDateTime.now();
        if (createDate == null) createDate = LocalDate.now();
    }
}
