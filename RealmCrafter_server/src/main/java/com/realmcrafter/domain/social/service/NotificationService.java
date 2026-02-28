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
}
