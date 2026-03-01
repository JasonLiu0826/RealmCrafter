package com.realmcrafter.domain.asset.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

/**
 * Fork 故事成功后发布，在事务提交后由监听器执行加经验与通知，避免与 fork 事务内的用户行锁冲突。
 */
@Getter
@RequiredArgsConstructor
public class StoryForkedEvent {

    /** 原作者；可为 null（如数据异常时仅给 fork 用户加经验） */
    private final Long authorUserId;
    private final Long forkUserId;
    private final String sourceStoryId;
    /** 创作者分润金额；免费 Fork 时为 0 */
    private final BigDecimal creatorAmount;
    private final boolean paid;

    public static StoryForkedEvent paid(Long authorUserId, Long forkUserId, String sourceStoryId, BigDecimal creatorAmount) {
        return new StoryForkedEvent(authorUserId, forkUserId, sourceStoryId, creatorAmount != null ? creatorAmount : BigDecimal.ZERO, true);
    }

    public static StoryForkedEvent free(Long authorUserId, Long forkUserId, String sourceStoryId) {
        return new StoryForkedEvent(authorUserId, forkUserId, sourceStoryId, BigDecimal.ZERO, false);
    }

    /** 仅给 fork 用户加经验（无原作者通知） */
    public static StoryForkedEvent forkUserOnly(Long forkUserId, String sourceStoryId) {
        return new StoryForkedEvent(null, forkUserId, sourceStoryId, BigDecimal.ZERO, false);
    }
}
