package com.realmcrafter.api.ad.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 广告观看完成回调请求。
 * 前端在用户观看完激励视频/插屏后，携带 451 响应中的 adToken 调用。
 */
@Data
public class AdCallbackRequest {

    @NotBlank(message = "adToken 不能为空")
    private String adToken;
}
