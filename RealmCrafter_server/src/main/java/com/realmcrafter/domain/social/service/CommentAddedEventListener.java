package com.realmcrafter.domain.social.service;

import com.realmcrafter.domain.social.event.CommentAddedEvent;
import com.realmcrafter.domain.user.ExpAction;
import com.realmcrafter.domain.user.service.UserExpService;
import com.realmcrafter.infrastructure.persistence.repository.StoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 评论事务提交后执行 @ 提及通知与加经验，避免与评论事务内用户行锁冲突导致 JDBC 异常。
 */
@Component
@RequiredArgsConstructor
public class CommentAddedEventListener {

    private final NotificationService notificationService;
    private final UserExpService userExpService;
    private final StoryRepository storyRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommentAdded(CommentAddedEvent event) {
        Long authorId = event.getCommentAuthorUserId();
        String excerpt = event.getContentExcerpt() != null && event.getContentExcerpt().length() > 50
                ? event.getContentExcerpt().substring(0, 50) + "…"
                : (event.getContentExcerpt() != null ? event.getContentExcerpt() : "");

        for (Long targetId : event.getMentionedUserIds()) {
            if (targetId != null && !targetId.equals(authorId)) {
                notificationService.sendMention(targetId, authorId, "COMMENT", String.valueOf(event.getCommentId()), excerpt);
                userExpService.addExp(targetId, ExpAction.BE_MENTIONED);
            }
        }

        storyRepository.findById(event.getStoryId()).ifPresent(story ->
                userExpService.addExp(story.getUserId(), ExpAction.BE_COMMENTED));
        userExpService.addExp(authorId, ExpAction.PUBLISH_COMMENT);
    }
}
