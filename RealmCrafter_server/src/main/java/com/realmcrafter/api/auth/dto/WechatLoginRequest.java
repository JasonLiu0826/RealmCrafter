package com.realmcrafter.api.auth.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 微信登录：生产环境应传 code（后端用 code 换 openid），当前支持直接传 openId 便于联调。
 */
@Data
public class WechatLoginRequest {

    @NotBlank(message = "openId 或 code 不能为空")
    private String openId;
}
