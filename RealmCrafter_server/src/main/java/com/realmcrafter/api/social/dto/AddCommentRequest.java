package com.realmcrafter.api.social.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 发表评论/回复请求。
 */
@Data
public class AddCommentRequest {

    @NotBlank(message = "storyId 不能为空")
    private String storyId;

    @NotNull(message = "chapterId 不能为空")
    private Long chapterId;

    @NotBlank(message = "评论内容不能为空")
    private String content;

    /** PARAGRAPH | OPTION | COMMENT */
    @NotBlank(message = "targetType 不能为空")
    private String targetType;

    /** 段落索引如 "2"、选项标识如 "0"、或回复时父评论 id 字符串 */
    private String targetRef;

    /** 回复时传父评论 id；顶级评论不传 */
    private Long parentCommentId;

    /** @提及 备份：前端解析到的用户 id 列表，与 content 中 @username 互补 */
    private List<Long> mentionedUserIds;
}
