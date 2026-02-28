package com.realmcrafter.api.ad;

import com.realmcrafter.api.dto.Result;
import com.realmcrafter.api.ad.dto.AdCallbackRequest;
import com.realmcrafter.infrastructure.redis.AdWatchTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 广告相关接口：观看完成回调，用于核销 451 时的一次性令牌并标记已观看，
 * 使下次心跳可继续生成章节。
 */
@RestController
@RequestMapping("/api/v1/ad")
@RequiredArgsConstructor
public class AdController {

    private final AdWatchTokenService adWatchTokenService;

    /**
     * 广告观看完成回调。
     * 前端展示广告结束后携带 451 响应中的 adToken 调用，后端核销令牌并设置 ad:watched，
     * 用户再次调用心跳即可继续生成。
     */
    @PostMapping("/callback")
    public Result<Void> callback(@Valid @RequestBody AdCallbackRequest request) {
        Long userId = adWatchTokenService.consumeToken(request.getAdToken());
        if (userId == null) {
            return Result.fail(400, "无效或已过期的 adToken");
        }
        adWatchTokenService.setAdWatched(userId);
        return Result.ok();
    }
}
