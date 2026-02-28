package com.realmcrafter.api.social;

import com.realmcrafter.api.dto.Result;
import com.realmcrafter.api.social.dto.GenerateShareRequest;
import com.realmcrafter.domain.social.service.ShareService;
import com.realmcrafter.infrastructure.persistence.entity.ShareRecordDO;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

/**
 * 转发/分享：生成深度链接与站内转发卡片 payload（消息模块 FORWARD_CARD 用）。
 */
@RestController
@RequestMapping("/api/v1/share")
@RequiredArgsConstructor
@Validated
public class ShareController {

    private final ShareService shareService;

    /**
     * 生成分享：返回短链、深度链接与站内转发卡片 payload（可发给好友）。
     */
    @PostMapping("/generate")
    public Result<Map<String, Object>> generate(@RequestBody @Valid GenerateShareRequest request) {
        ShareRecordDO.ShareType type = parseShareType(request.getType());
        ShareService.ShareResult result = shareService.generate(
                type,
                request.getStoryId(),
                request.getChapterId(),
                request.getTargetRef(),
                request.getExcerpt()
        );
        Map<String, Object> data = Map.of(
                "shortCode", result.shortCode,
                "deepLink", result.deepLink,
                "forwardCardPayload", result.forwardCardPayload
        );
        return Result.ok(data);
    }

    /**
     * 解析短链：App 打开深度链接后调用，拿到锚点信息并定位到段落/选项/评论。
     */
    @GetMapping("/decode/{shortCode}")
    public Result<ShareService.ShareTarget> decode(@PathVariable String shortCode) {
        return shareService.decode(shortCode)
                .map(Result::ok)
                .orElseGet(() -> Result.fail(404, "链接无效或已过期"));
    }

    private static ShareRecordDO.ShareType parseShareType(String type) {
        if (type == null) return ShareRecordDO.ShareType.PARAGRAPH;
        switch (type.toUpperCase()) {
            case "OPTION": return ShareRecordDO.ShareType.OPTION;
            case "COMMENT": return ShareRecordDO.ShareType.COMMENT;
            default: return ShareRecordDO.ShareType.PARAGRAPH;
        }
    }
}
