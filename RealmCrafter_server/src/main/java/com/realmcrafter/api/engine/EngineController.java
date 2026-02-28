package com.realmcrafter.api.engine;

import com.realmcrafter.api.engine.dto.GenerateStreamRequest;
import com.realmcrafter.application.chapter.ChapterApplicationService;
import com.realmcrafter.infrastructure.llm.dto.StreamChunk;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.IOException;
import java.util.Map;

/**
 * AI 章节生成引擎：SSE 流式接口。
 * POST /api/v1/engine/generate/stream
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/engine")
@RequiredArgsConstructor
@Validated
public class EngineController {

    private static final long SSE_TIMEOUT_MS = 5 * 60 * 1000L;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ChapterApplicationService chapterApplicationService;

    @PostMapping(value = "/generate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter generateStream(@RequestBody @Valid GenerateStreamRequest request,
                                    @RequestHeader(value = "X-User-Id", required = false) Long userId,
                                    HttpServletRequest httpRequest) {
        if (userId == null) {
            throw new IllegalArgumentException("缺少 X-User-Id 请求头");
        }

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        emitter.onCompletion(() -> log.debug("SSE completed: storyId={}", request.getStoryId()));
        emitter.onTimeout(() -> log.warn("SSE timeout: storyId={}", request.getStoryId()));
        emitter.onError(e -> log.warn("SSE error: storyId={}", request.getStoryId(), e));

        new Thread(() -> {
            try {
                chapterApplicationService.generateStream(
                        request.getStoryId(),
                        userId,
                        request.getUserChoice(),
                        request.getChaosLevel() != null ? request.getChaosLevel() : 0.7,
                        Boolean.TRUE.equals(request.getUseByok()),
                        chunk -> sendChunk(emitter, chunk)
                );
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().name("error").data(Map.of("message", e.getMessage())));
                } catch (IOException ignored) {}
                emitter.completeWithError(e);
            }
        }).start();

        return emitter;
    }

    private void sendChunk(SseEmitter emitter, StreamChunk chunk) {
        try {
            switch (chunk.getType()) {
                case CONTENT:
                    emitter.send(SseEmitter.event().data(MAPPER.writeValueAsString(Map.of("content", chunk.getContent()))));
                    break;
                case BRANCHES:
                    emitter.send(SseEmitter.event().data(MAPPER.writeValueAsString(Map.of("branches", chunk.getBranches()))));
                    break;
                case DONE:
                    emitter.send(SseEmitter.event().data("[DONE]"));
                    break;
                default:
                    break;
            }
        } catch (IOException e) {
            throw new RuntimeException("SSE send failed", e);
        }
    }
}
