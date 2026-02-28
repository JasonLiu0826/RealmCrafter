package com.realmcrafter.api.auth.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 苹果登录：生产环境需传 identityToken 后端验签；当前支持传 appleId（唯一标识）便于联调。
 */
@Data
public class AppleLoginRequest {

    @NotBlank(message = "appleId 不能为空")
    private String appleId;
}
