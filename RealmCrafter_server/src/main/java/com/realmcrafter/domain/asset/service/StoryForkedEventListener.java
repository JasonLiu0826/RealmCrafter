package com.realmcrafter.domain.asset.service;

import com.realmcrafter.domain.asset.event.StoryForkedEvent;
import com.realmcrafter.domain.user.ExpAction;
import com.realmcrafter.domain.user.service.UserExpService;
import com.realmcrafter.domain.social.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Fork 事务提交后执行加经验与通知，避免与 fork 内用户行锁冲突导致 PessimisticLockException。
 */
@Component
@RequiredArgsConstructor
public class StoryForkedEventListener {

    private final UserExpService userExpService;
    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStoryForked(StoryForkedEvent event) {
        Long authorId = event.getAuthorUserId();
        Long forkUserId = event.getForkUserId();
        String sourceStoryId = event.getSourceStoryId();

        if (authorId != null) {
            if (event.isPaid()) {
                userExpService.addExp(authorId, ExpAction.BE_BOUGHT);
                notificationService.sendReward(authorId, forkUserId, "STORY", sourceStoryId,
                        "你的付费作品被收藏", "获得 " + event.getCreatorAmount() + " 灵能水晶");
            } else {
                userExpService.addExp(authorId, ExpAction.BE_FORKED);
                notificationService.sendReward(authorId, forkUserId, "STORY", sourceStoryId,
                        "你的作品被收藏", "获得 15 经验");
            }
        }
        userExpService.addExp(forkUserId, ExpAction.FORK_ASSET);
    }
}
