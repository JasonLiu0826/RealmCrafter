package com.realmcrafter.domain.social.service;

import com.realmcrafter.infrastructure.persistence.entity.SystemNotificationDO;
import com.realmcrafter.infrastructure.persistence.repository.SystemNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 系统通知：@提及、互动、奖励等，写入 system_notification 表供前端拉取。
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SystemNotificationRepository systemNotificationRepository;

    /**
     * 分页查询用户通知，类型可选：SYSTEM、MENTION、INTERACTION、REWARD；空或 null 表示全部。
     */
    @Transactional(readOnly = true)
    public Page<SystemNotificationDO> listNotifications(Long userId, String type, Pageable pageable) {
        if (type == null || type.isBlank()) {
            return systemNotificationRepository.findByUserIdOrderByCreateTimeDesc(userId, pageable);
        }
        SystemNotificationDO.NotificationType typeEnum;
        try {
            typeEnum = SystemNotificationDO.NotificationType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return systemNotificationRepository.findByUserIdOrderByCreateTimeDesc(userId, pageable);
        }
        return systemNotificationRepository.findByUserIdAndTypeOrderByCreateTimeDesc(userId, typeEnum, pageable);
    }

    /** 标记单条通知已读（仅本人） */
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        systemNotificationRepository.markAsReadByIdAndUserId(notificationId, userId);
    }

    /** 标记该用户全部通知已读 */
    @Transactional
    public void markAllAsRead(Long userId) {
        systemNotificationRepository.markAllAsReadByUserId(userId);
    }

    @Transactional
    public void sendMention(Long targetUserId, Long actorUserId, String refType, String refId, String excerpt) {
        if (targetUserId == null || targetUserId.equals(actorUserId)) return;
        SystemNotificationDO n = new SystemNotificationDO();
        n.setUserId(targetUserId);
        n.setType(SystemNotificationDO.NotificationType.MENTION);
        n.setTitle("有人提到了你");
        n.setBody(excerpt != null && !excerpt.isEmpty() ? excerpt : "点击查看");
        if (n.getBody() == null) n.setBody("");
        n.setRefType(refType);
        n.setRefId(refId);
        n.setActorUserId(actorUserId);
        systemNotificationRepository.save(n);
    }

    /**
     * 发送创作者等级跃迁系统通知。
     */
    @Transactional
    public void sendLevelUp(Long userId, int newLevel) {
        if (userId == null) return;
        SystemNotificationDO n = new SystemNotificationDO();
        n.setUserId(userId);
        n.setType(SystemNotificationDO.NotificationType.LEVEL_UP);
        n.setTitle("创作者等级提升");
        n.setBody("恭喜！您已晋升为 Lv" + newLevel + " 创作者");
        n.setRefType("LEVEL");
        n.setRefId(String.valueOf(newLevel));
        systemNotificationRepository.save(n);
    }

    /**
     * 作品被叉（付费/免费）时发送 REWARD 通知给作者：获得经验或水晶收益。
     *
     * @param targetUserId 作品作者
     * @param actorUserId  叉作品的用户
     * @param refType      如 STORY、SETTING
     * @param refId        作品 ID
     * @param title        标题（如「你的作品被收藏了」）
     * @param body         正文（如「获得 50 经验」或「获得 10 水晶」）
     */
    @Transactional
    public void sendReward(Long targetUserId, Long actorUserId, String refType, String refId, String title, String body) {
        if (targetUserId == null || targetUserId.equals(actorUserId)) return;
        SystemNotificationDO n = new SystemNotificationDO();
        n.setUserId(targetUserId);
        n.setType(SystemNotificationDO.NotificationType.REWARD);
        n.setTitle(title != null && !title.isBlank() ? title : "作品被收藏");
        n.setBody(body != null && !body.isBlank() ? body : "点击查看");
        if (n.getBody() == null) n.setBody("");
        n.setRefType(refType);
        n.setRefId(refId);
        n.setActorUserId(actorUserId);
        systemNotificationRepository.save(n);
    }
}
