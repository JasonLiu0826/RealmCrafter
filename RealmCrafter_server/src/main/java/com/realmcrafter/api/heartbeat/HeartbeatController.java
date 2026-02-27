package com.realmcrafter.api.heartbeat;

import com.realmcrafter.api.dto.Result;
import com.realmcrafter.application.heartbeat.HeartbeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 接收前端心跳/生成章节前请求：根据当前登录用户更新互动计数，
 * 当满足广告触发条件时由领域层抛出 AdTriggerRequiredException，
 * 交由全局异常处理器返回 451，前端据此拉起广告。
 */
@RestController
@RequestMapping("/api/v1/heartbeat")
@RequiredArgsConstructor
public class HeartbeatController {

    private final HeartbeatService heartbeatService;

    /**
     * 生成章节前上报心跳。
     * <p>
     * 用户身份由 Spring Security + JWT 负责解析和注入，
     * HeartbeatService 内部通过安全上下文获取当前用户并执行业务逻辑，
     * 本控制器不接受任何明文用户 ID 参数，避免越权风险。
     */
    @PostMapping("/chapter-generate")
    public ResponseEntity<Result<Void>> heartbeat() {
        heartbeatService.heartbeat();
        return ResponseEntity.ok(Result.ok());
    }
}
