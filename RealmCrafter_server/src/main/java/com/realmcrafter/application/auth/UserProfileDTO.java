package com.realmcrafter.application.auth;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户资料（对外暴露，不含密码等敏感字段）。
 */
@Data
public class UserProfileDTO {

    private Long userId;
    private String username;
    private String role;
    private String nickname;
    private String signature;
    private String avatar;
    private Integer level;
    private Long exp;
    private Boolean isGoldenCreator;
    private LocalDateTime vipExpireTime;
    private Long tokenBalance;
    private BigDecimal crystalBalance;
}
