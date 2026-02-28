package com.realmcrafter.api.engine.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * POST /api/v1/engine/generate/stream 请求体。
 */
@Data
public class GenerateStreamRequest {

    @NotBlank(message = "storyId 不能为空")
    private String storyId;

    /** 用户选择或自定义输入，如「进入深渊之门」 */
    private String userChoice = "";

    /** 对应 LLM Temperature，0.0～1.0，默认 0.7 */
    @NotNull
    private Double chaosLevel = 0.7;

    /** 是否使用用户自己的 Key（BYOK） */
    private Boolean useByok = false;
}
