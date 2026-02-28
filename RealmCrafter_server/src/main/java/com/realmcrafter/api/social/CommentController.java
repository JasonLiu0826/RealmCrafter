package com.realmcrafter.api.social;

import com.realmcrafter.api.dto.Result;
import com.realmcrafter.api.social.dto.AddCommentRequest;
import com.realmcrafter.domain.social.service.CommentService;
import com.realmcrafter.infrastructure.persistence.entity.CommentDO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 段评/选项评/楼中楼：增删改查。
 */
@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
@Validated
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    public Result<CommentDO> add(@RequestHeader("X-User-Id") Long userId,
                                 @RequestBody @Valid AddCommentRequest request) {
        CommentDO.TargetType targetType = parseTargetType(request.getTargetType());
        String targetRef = request.getTargetRef() != null ? request.getTargetRef() : "";
        Long parentId = request.getParentCommentId();
        if (parentId != null && targetType != CommentDO.TargetType.COMMENT) {
            targetType = CommentDO.TargetType.COMMENT;
            targetRef = String.valueOf(parentId);
        }
        CommentDO comment = commentService.addComment(
                request.getStoryId(),
                request.getChapterId(),
                userId,
                request.getContent(),
                targetType,
                targetRef,
                parentId,
                request.getMentionedUserIds()
        );
        return Result.ok(comment);
    }

    /**
     * 按锚点分页拉取一级评论。
     * @param storyId 故事 id
     * @param chapterId 章节 id
     * @param targetType PARAGRAPH | OPTION
     * @param targetRef 段落索引或选项标识
     */
    @GetMapping("/anchor")
    public Result<Page<CommentDO>> listByAnchor(@RequestParam String storyId,
                                                @RequestParam Long chapterId,
                                                @RequestParam String targetType,
                                                @RequestParam(required = false) String targetRef,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<CommentDO> list = commentService.listCommentsByAnchor(
                storyId,
                chapterId,
                parseTargetType(targetType),
                targetRef != null ? targetRef : "",
                pageable
        );
        return Result.ok(list);
    }

    /** 某条顶级评论下的所有楼中楼 */
    @GetMapping("/replies/{rootCommentId}")
    public Result<List<CommentDO>> listReplies(@PathVariable Long rootCommentId) {
        return Result.ok(commentService.listReplies(rootCommentId));
    }

    @GetMapping("/{commentId}")
    public Result<CommentDO> get(@PathVariable Long commentId) {
        return commentService.getComment(commentId)
                .map(Result::ok)
                .orElseGet(() -> Result.fail(404, "评论不存在"));
    }

    @DeleteMapping("/{commentId}")
    public Result<Void> delete(@RequestHeader("X-User-Id") Long userId, @PathVariable Long commentId) {
        commentService.deleteComment(commentId, userId);
        return Result.ok();
    }

    private static CommentDO.TargetType parseTargetType(String type) {
        if (type == null) return CommentDO.TargetType.PARAGRAPH;
        switch (type.toUpperCase()) {
            case "OPTION": return CommentDO.TargetType.OPTION;
            case "COMMENT": return CommentDO.TargetType.COMMENT;
            default: return CommentDO.TargetType.PARAGRAPH;
        }
    }
}
