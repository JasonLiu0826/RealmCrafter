package com.realmcrafter.domain.social.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * 评论发表成功后发布，在事务提交后由监听器执行 @ 提及通知与加经验，避免与评论事务内用户行锁冲突。
 */
@Getter
@RequiredArgsConstructor
public class CommentAddedEvent {

    private final Long commentId;
    private final String storyId;
    private final Long commentAuthorUserId;
    /** 提及通知摘要（如评论前 50 字） */
    private final String contentExcerpt;
    /** 被 @ 的用户 ID 列表 */
    private final List<Long> mentionedUserIds;

    public List<Long> getMentionedUserIds() {
        return mentionedUserIds != null ? mentionedUserIds : Collections.emptyList();
    }
}
