package com.realmcrafter.api.social;

import com.realmcrafter.api.dto.Result;
import com.realmcrafter.domain.social.service.NotificationService;
import com.realmcrafter.infrastructure.persistence.entity.SystemNotificationDO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 系统通知：列表查询、标记已读。
 * 需登录，请求头 X-User-Id 传递当前用户。
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Validated
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 分页获取当前用户的通知列表。
     * @param type 可选：SYSTEM、MENTION、INTERACTION、REWARD；不传或空表示全部
     */
    @GetMapping
    public Result<Page<SystemNotificationDO>> list(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<SystemNotificationDO> list = notificationService.listNotifications(userId, type, pageable);
        return Result.ok(list);
    }

    /** 标记单条通知已读 */
    @PatchMapping("/{id}/read")
    public Result<Void> markAsRead(@RequestHeader("X-User-Id") Long userId,
                                   @PathVariable Long id) {
        notificationService.markAsRead(id, userId);
        return Result.ok();
    }

    /** 标记当前用户全部通知已读 */
    @PatchMapping("/read-all")
    public Result<Void> markAllAsRead(@RequestHeader("X-User-Id") Long userId) {
        notificationService.markAllAsRead(userId);
        return Result.ok();
    }
}
