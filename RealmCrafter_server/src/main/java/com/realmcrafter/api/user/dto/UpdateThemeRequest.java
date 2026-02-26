package com.realmcrafter.api.user.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * PATCH /api/v1/users/me/theme 请求体。
 */
@Data
public class UpdateThemeRequest {

    /**
     * 目标主题 ID，例如 classic_white、hacker_green 等。
     */
    @NotBlank(message = "themeId 不能为空")
    private String themeId;

    /**
     * 前端传入的设备指纹，用于后续多端冲突与风控。
     * 目前服务端仅透传保留字段，未参与业务校验。
     */
    private String deviceHash;
}

