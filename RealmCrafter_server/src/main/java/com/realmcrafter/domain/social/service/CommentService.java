package com.realmcrafter.domain.social.service;

import com.realmcrafter.domain.social.event.CommentAddedEvent;
import com.realmcrafter.infrastructure.persistence.entity.CommentDO;
import com.realmcrafter.infrastructure.persistence.entity.UserDO;
import com.realmcrafter.infrastructure.persistence.repository.CommentRepository;
import com.realmcrafter.infrastructure.persistence.repository.StoryRepository;
import com.realmcrafter.infrastructure.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 段评/选项评/楼中楼评论服务。支持 @提及 解析并写入系统通知。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private static final Pattern MENTION_PATTERN = Pattern.compile("@([\\w\\u4e00-\\u9fa5]{1,32})");

    private final CommentRepository commentRepository;
    private final StoryRepository storyRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 发表评论或回复。若为回复则 parentCommentId 非空，并会更新父评论 replyCount；保存前解析 @username 发送提及通知。
     */
    @Transactional
    public CommentDO addComment(String storyId, Long chapterId, Long userId, String content,
                                CommentDO.TargetType targetType, String targetRef,
                                Long parentCommentId, List<Long> mentionedUserIds) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("评论内容不能为空");
        }
        if (!storyRepository.existsById(storyId)) {
            throw new IllegalArgumentException("故事不存在");
        }

        CommentDO comment = new CommentDO();
        comment.setStoryId(storyId);
        comment.setChapterId(chapterId);
        comment.setUserId(userId);
        comment.setContent(content.trim());
        comment.setTargetType(targetType);
        comment.setTargetRef(targetRef != null ? targetRef : "");

        if (parentCommentId != null && targetType == CommentDO.TargetType.COMMENT) {
            CommentDO parent = commentRepository.findById(parentCommentId)
                    .orElseThrow(() -> new IllegalArgumentException("父评论不存在"));
            if (parent.getStatus() != CommentDO.Status.NORMAL) {
                throw new IllegalArgumentException("无法回复已删除的评论");
            }
            comment.setParentCommentId(parentCommentId);
            comment.setRootCommentId(parent.getRootCommentId() != null ? parent.getRootCommentId() : parentCommentId);
            commentRepository.save(comment);
            incrementReplyCount(parent.getRootCommentId() != null ? parent.getRootCommentId() : parentCommentId);
        } else {
            comment.setRootCommentId(null);
            comment.setParentCommentId(null);
            commentRepository.save(comment);
        }

        List<Long> toNotify = resolveMentionedUserIds(content, mentionedUserIds);
        // 事务提交后执行提及通知与加经验，避免与当前事务内用户行锁冲突
        eventPublisher.publishEvent(new CommentAddedEvent(
                comment.getId(),
                storyId,
                userId,
                content.trim(),
                toNotify
        ));

        return comment;
    }

    private void incrementReplyCount(Long rootCommentId) {
        commentRepository.findById(rootCommentId).ifPresent(root -> {
            root.setReplyCount((root.getReplyCount() != null ? root.getReplyCount() : 0) + 1);
            commentRepository.save(root);
        });
    }

    private List<Long> resolveMentionedUserIds(String content, List<Long> backupIds) {
        List<Long> ids = new ArrayList<>();
        if (backupIds != null && !backupIds.isEmpty()) {
            ids.addAll(backupIds);
        }
        Matcher m = MENTION_PATTERN.matcher(content);
        while (m.find()) {
            String username = m.group(1);
            userRepository.findByUsername(username).map(UserDO::getId).ifPresent(id -> {
                if (!ids.contains(id)) ids.add(id);
            });
        }
        return ids;
    }

    /** 按锚点分页拉取一级评论（段落/选项下的顶级评论） */
    public Page<CommentDO> listCommentsByAnchor(String storyId, Long chapterId,
                                                 CommentDO.TargetType targetType, String targetRef,
                                                 Pageable pageable) {
        return commentRepository.findByStoryIdAndChapterIdAndTargetTypeAndTargetRefAndRootCommentIdIsNullAndStatus(
                storyId, chapterId, targetType, targetRef != null ? targetRef : "", CommentDO.Status.NORMAL, pageable);
    }

    /** 某条顶级评论下的所有楼中楼 */
    public List<CommentDO> listReplies(Long rootCommentId) {
        return commentRepository.findByRootCommentIdAndStatusOrderByCreateTimeAsc(rootCommentId, CommentDO.Status.NORMAL);
    }

    public Optional<CommentDO> getComment(Long commentId) {
        return commentRepository.findById(commentId);
    }

    /** 软删除：仅评论作者可删 */
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        CommentDO c = commentRepository.findById(commentId).orElseThrow(() -> new IllegalArgumentException("评论不存在"));
        if (!c.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权删除该评论");
        }
        c.setStatus(CommentDO.Status.DELETED);
        commentRepository.save(c);
    }
}
