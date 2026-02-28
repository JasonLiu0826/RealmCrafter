package com.realmcrafter.infrastructure.llm.dto;

import lombok.Builder;
import lombok.Data;

/**
 * LLM 流式请求：系统提示（L1+L2 拼装）、用户选择/输入、温度、BYOK Key。
 */
@Data
@Builder
public class LlmStreamRequest {

    /** 系统消息：设定集 + 近期章节等绝对/短期记忆 */
    private String systemPrompt;
    /** 用户当前动作/选择项 */
    private String userMessage;
    /** 对应 LLM temperature，0.0～1.0 */
    private double temperature;
    /** BYOK 时传入用户自己的 apiKey，否则 null 用平台 Key */
    private String apiKey;
}
