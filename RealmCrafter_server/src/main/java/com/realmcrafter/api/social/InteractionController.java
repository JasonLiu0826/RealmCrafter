package com.realmcrafter.api.social;

import com.realmcrafter.api.dto.Result;
import com.realmcrafter.domain.social.service.InteractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 社交互动原子接口：点赞/取消赞、收藏/取消收藏，幂等。
 */
@RestController
@RequestMapping("/api/v1/interactions")
@RequiredArgsConstructor
@Validated
public class InteractionController {

    private final InteractionService interactionService;

    @PostMapping("/like")
    public Result<Map<String, Object>> like(@RequestHeader("X-User-Id") Long userId,
                                            @RequestBody Map<String, String> body) {
        String type = body.get("type");
        String id = body.get("id");
        if (type == null || id == null) {
            throw new IllegalArgumentException("type 与 id 不能为空");
        }
        boolean liked = interactionService.toggleLike(userId, type, id.trim());
        return Result.ok(Map.of("liked", liked, "type", type, "id", id));
    }

    @PostMapping("/favorite")
    public Result<Map<String, Object>> favorite(@RequestHeader("X-User-Id") Long userId,
                                                @RequestBody Map<String, String> body) {
        String type = body.get("type");
        String id = body.get("id");
        if (type == null || id == null) {
            throw new IllegalArgumentException("type 与 id 不能为空");
        }
        boolean favorited = interactionService.toggleFavorite(userId, type, id.trim());
        return Result.ok(Map.of("favorited", favorited, "type", type, "id", id));
    }
}
