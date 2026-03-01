package com.realmcrafter.domain.social.service;

import com.realmcrafter.domain.user.ExpAction;
import com.realmcrafter.domain.user.service.UserExpService;
import com.realmcrafter.infrastructure.persistence.repository.StoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;

/**
 * 在独立线程中执行通知与加经验，由 afterCommit 回调触发，避免与主事务行锁冲突且不依赖同步回调执行顺序。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AfterCommitNotificationRunner {

    private final NotificationService notificationService;
    private final UserExpService userExpService;
    private final StoryRepository storyRepository;
    private final PlatformTransactionManager transactionManager;

    /** 在独立事务中执行通知写入并立即提交，避免在 afterCommit 回调中事务未正确提交 */
    private void runInNewTransaction(Runnable action) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tx.executeWithoutResult(status -> action.run());
    }

    /** 在 afterCommit 回调中同步调用，确保通知落库后再返回响应 */
    public void runForkRewardAndExp(Long authorId, Long forkUserId, String sourceStoryId,
                                    BigDecimal creatorAmountForNotify, boolean paid) {
        try {
            if (authorId != null) {
                if (paid) {
                    runInNewTransaction(() -> notificationService.sendReward(authorId, forkUserId, "STORY", sourceStoryId,
                            "你的付费作品被收藏", "获得 " + creatorAmountForNotify + " 灵能水晶"));
                    userExpService.addExp(authorId, ExpAction.BE_BOUGHT);
                } else {
                    runInNewTransaction(() -> notificationService.sendReward(authorId, forkUserId, "STORY", sourceStoryId,
                            "你的作品被收藏", "获得 15 经验"));
                    userExpService.addExp(authorId, ExpAction.BE_FORKED);
                }
            }
            userExpService.addExp(forkUserId, ExpAction.FORK_ASSET);
        } catch (Exception e) {
            log.warn("Fork 后通知/经验执行异常: authorId={}, forkUserId={}", authorId, forkUserId, e);
        }
    }

    /** 在 afterCommit 回调中同步调用 */
    public void runCommentMentionAndExp(long commentId, String storyId, long commentAuthorUserId,
                                        String excerpt, List<Long> mentionedUserIds) {
        try {
            String excerptTrim = excerpt != null && excerpt.length() > 50 ? excerpt.substring(0, 50) + "…" : (excerpt != null ? excerpt : "");
            for (Long targetId : mentionedUserIds) {
                if (targetId != null && targetId != commentAuthorUserId) {
                    runInNewTransaction(() -> notificationService.sendMention(targetId, commentAuthorUserId, "COMMENT", String.valueOf(commentId), excerptTrim));
                    userExpService.addExp(targetId, ExpAction.BE_MENTIONED);
                }
            }
            // 在独立事务中按 ID 查 story 再取 authorId，避免使用主事务的 proxy 导致 LazyInitializationException
            runInNewTransaction(() ->
                    storyRepository.findById(storyId).ifPresent(story ->
                            userExpService.addExp(story.getUserId(), ExpAction.BE_COMMENTED)));
            userExpService.addExp(commentAuthorUserId, ExpAction.PUBLISH_COMMENT);
        } catch (Exception e) {
            log.warn("评论后提及/经验执行异常: commentId={}, storyId={}", commentId, storyId, e);
        }
    }
}
