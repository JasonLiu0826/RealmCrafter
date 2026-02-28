package com.realmcrafter.domain.social.service;

import com.realmcrafter.infrastructure.persistence.entity.SystemNotificationDO;
import com.realmcrafter.infrastructure.persistence.repository.SystemNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 系统通知：@提及 等，写入 system_notification 表供前端拉取。
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SystemNotificationRepository systemNotificationRepository;

    @Transactional
    public void sendMention(Long targetUserId, Long actorUserId, String refType, String refId, String excerpt) {
        if (targetUserId == null || targetUserId.equals(actorUserId)) return;
        SystemNotificationDO n = new SystemNotificationDO();
        n.setUserId(targetUserId);
        n.setType(SystemNotificationDO.NotificationType.MENTION);
        n.setTitle("有人提到了你");
        n.setBody(excerpt != null && !excerpt.isEmpty() ? excerpt : "点击查看");
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
}
