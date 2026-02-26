package com.realmcrafter.api.user;

import com.realmcrafter.api.dto.Result;
import com.realmcrafter.api.user.dto.UpdateThemeRequest;
import com.realmcrafter.application.user.ThemeApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 用户偏好相关接口。
 */
@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
@Validated
public class UserPreferenceController {

    private final ThemeApplicationService themeApplicationService;

    /**
     * 切换当前用户的主题。
     *
     * TODO：当前通过请求头 X-User-Id 获取用户 ID，后续可接入统一鉴权上下文。
     */
    @PatchMapping("/theme")
    public Result<Void> updateTheme(@RequestBody @Valid UpdateThemeRequest request,
                                    @RequestHeader("X-User-Id") Long userId) {
        themeApplicationService.updateUserTheme(userId, request.getThemeId());
        return Result.ok();
    }
}

