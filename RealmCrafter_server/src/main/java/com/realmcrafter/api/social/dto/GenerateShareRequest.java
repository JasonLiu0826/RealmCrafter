package com.realmcrafter.api.social.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 生成转发/分享链接请求（段落、选项或评论）。
 */
@Data
public class GenerateShareRequest {

    /** PARAGRAPH | OPTION | COMMENT */
    @NotBlank(message = "type 不能为空")
    private String type;

    @NotBlank(message = "storyId 不能为空")
    private String storyId;

    @NotNull(message = "chapterId 不能为空")
    private Long chapterId;

    /** 段落索引、选项标识或评论 id 字符串 */
    private String targetRef;

    /** 摘录文案，用于转发卡片展示 */
    private String excerpt;
}
