package com.realmcrafter.api.admin;

import com.realmcrafter.api.dto.Result;
import com.realmcrafter.application.admin.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 平台管理后台：封禁用户、下架故事、授予金牌创作者。
 * 上帝权限校验：仅 UserDO.role 为 ADMIN / SUPER_ADMIN 可调用，否则 403。
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    /** 封禁用户：targetUserId 为目标用户，days 为封禁天数，sealedUntil = 当前时间 + days */
    @PostMapping("/seal-user")
    public Result<Void> sealUser(
            @RequestParam(name = "targetUserId") Long targetUserId,
            @RequestParam int days) {
        Long adminId = resolveCurrentUserId();
        if (adminId == null || !adminService.isAdmin(adminId)) {
            return Result.fail(403, "无权限");
        }
        LocalDateTime sealedUntil = LocalDateTime.now().plusDays(days);
        return adminService.sealUser(targetUserId, sealedUntil)
                .map(err -> Result.<Void>fail(400, err))
                .orElseGet(() -> Result.ok(null));
    }

    @PostMapping("/take-down-story")
    public Result<Void> takeDownStory(@RequestParam String storyId) {
        Long adminId = resolveCurrentUserId();
        if (adminId == null || !adminService.isAdmin(adminId)) {
            return Result.fail(403, "无权限");
        }
        return adminService.takeDownStory(storyId)
                .map(err -> Result.<Void>fail(400, err))
                .orElseGet(() -> Result.ok(null));
    }

    /** 手动授予金牌创作者：isGoldenCreator = true，享受 90% 分润与高流量权重 */
    @PostMapping("/grant-golden-creator")
    public Result<Void> grantGoldenCreator(@RequestParam(name = "targetUserId") Long targetUserId) {
        Long adminId = resolveCurrentUserId();
        if (adminId == null || !adminService.isAdmin(adminId)) {
            return Result.fail(403, "无权限");
        }
        return adminService.grantGoldenCreator(targetUserId)
                .map(err -> Result.<Void>fail(400, err))
                .orElseGet(() -> Result.ok(null));
    }

    private static Long resolveCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) return null;
        Object p = auth.getPrincipal();
        if (p instanceof Long) return (Long) p;
        if (p instanceof String) {
            try { return Long.parseLong((String) p); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }
}
