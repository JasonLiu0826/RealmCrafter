package com.realmcrafter.infrastructure.persistence.repository;

import com.realmcrafter.infrastructure.persistence.entity.CommentDO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<CommentDO, Long> {

    /** 按锚点拉取一级评论（顶级评论，root_comment_id 为空），按时间倒序 */
    Page<CommentDO> findByStoryIdAndChapterIdAndTargetTypeAndTargetRefAndRootCommentIdIsNullAndStatus(
            String storyId, Long chapterId, CommentDO.TargetType targetType, String targetRef,
            CommentDO.Status status, Pageable pageable);

    /** 某条顶级评论下的所有楼中楼，按时间正序 */
    List<CommentDO> findByRootCommentIdAndStatusOrderByCreateTimeAsc(Long rootCommentId, CommentDO.Status status);

    boolean existsByIdAndStatus(Long id, CommentDO.Status status);
}
