package com.realmcrafter.infrastructure.llm;

import com.realmcrafter.infrastructure.llm.dto.LlmStreamRequest;
import com.realmcrafter.infrastructure.llm.dto.StreamChunk;

import java.util.function.Consumer;

/**
 * LLM 客户端抽象：支持多模型路由（DeepSeek、GPT、Claude）与 BYOK（用户自有 Key）。
 * 契约：流式输出 + 强制 JSON 模式，确保 100% 返回可解析的包装数据。
 */
public interface LlmClient {

    /**
     * 流式生成：向大模型发起请求，每解析出一块 content 或 branches 即通过 consumer 回调。
     *
     * @param request 请求参数（系统提示、用户输入、temperature、可选 apiKey）
     * @param chunkConsumer 每收到 content 片段或 branches 时调用
     * @return 实际消耗的 total_tokens（用于事后结算），若未返回则可为 null
     */
    Long stream(LlmStreamRequest request, Consumer<StreamChunk> chunkConsumer) throws Exception;
}
