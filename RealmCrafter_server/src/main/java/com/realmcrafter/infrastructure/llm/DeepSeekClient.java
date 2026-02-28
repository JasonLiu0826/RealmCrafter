package com.realmcrafter.infrastructure.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.realmcrafter.infrastructure.llm.dto.LlmStreamRequest;
import com.realmcrafter.infrastructure.llm.dto.StreamChunk;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * DeepSeek 流式客户端：SSE 连接第三方模型，维持与上游的流式转发。
 * 契约：系统提示中约定结尾输出 BRANCHES:\n["选项A","选项B","选项C"]，便于解析分支；
 * 不强制 API 层 json_object，以支持逐 token 的 content 流。
 */
@Slf4j
@Component
public class DeepSeekClient implements LlmClient {

    private static final String BRANCHES_MARKER = "BRANCHES:\n";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Value("${realmcrafter.llm.deepseek.url:https://api.deepseek.com}")
    private String baseUrl;

    @Value("${realmcrafter.llm.deepseek.model:deepseek-chat}")
    private String model;

    @Value("${realmcrafter.llm.deepseek.api-key:}")
    private String platformApiKey;

    private final HttpClient httpClient = HttpClient.newBuilder().build();

    @Override
    public Long stream(LlmStreamRequest request, Consumer<StreamChunk> chunkConsumer) throws Exception {
        String apiKey = request.getApiKey() != null && !request.getApiKey().isBlank()
                ? request.getApiKey()
                : platformApiKey;
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("DeepSeek API Key 未配置（平台 Key 或 BYOK）");
        }

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", request.getSystemPrompt()),
                        Map.of("role", "user", "content", request.getUserMessage())
                ),
                "stream", true,
                "temperature", Math.max(0, Math.min(1, request.getTemperature())),
                "max_tokens", 4096,
                "stream_options", Map.of("include_usage", true)
        );

        StringBuilder contentBuffer = new StringBuilder();
        StringBuilder branchesBuffer = new StringBuilder();
        boolean inBranches = false;
        int bracketBalance = 0;
        Long[] totalTokens = { null };

        String jsonBody = MAPPER.writeValueAsString(body);
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/chat/completions"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<java.io.InputStream> response = httpClient.send(httpRequest,
                HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() != 200) {
            throw new IllegalStateException("DeepSeek API 非 200: " + response.statusCode());
        }

        try (var reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.startsWith("data: ")) continue;
                String data = line.substring(6).trim();
                if ("[DONE]".equals(data)) break;

                try {
                    JsonNode root = MAPPER.readTree(data);
                    JsonNode choices = root.get("choices");
                    if (choices != null && choices.isArray() && choices.size() > 0) {
                        JsonNode delta = choices.get(0).get("delta");
                        if (delta != null && delta.has("content")) {
                            String piece = delta.get("content").asText("");
                            if (piece.isEmpty()) continue;

                            if (inBranches) {
                                branchesBuffer.append(piece);
                                for (char c : piece.toCharArray()) {
                                    if (c == '[') bracketBalance++;
                                    else if (c == ']') bracketBalance--;
                                }
                                if (bracketBalance <= 0) {
                                    inBranches = false;
                                    List<String> branches = parseBranchesArray(branchesBuffer.toString());
                                    if (!branches.isEmpty()) chunkConsumer.accept(StreamChunk.branches(branches));
                                    branchesBuffer.setLength(0);
                                }
                                continue;
                            }

                            int idx = piece.indexOf(BRANCHES_MARKER);
                            if (idx >= 0) {
                                String before = piece.substring(0, idx);
                                if (!before.isEmpty()) {
                                    contentBuffer.append(before);
                                    chunkConsumer.accept(StreamChunk.content(before));
                                }
                                String after = piece.substring(idx + BRANCHES_MARKER.length());
                                inBranches = true;
                                bracketBalance = 0;
                                branchesBuffer.setLength(0);
                                branchesBuffer.append(after);
                                for (char c : after.toCharArray()) {
                                    if (c == '[') bracketBalance++;
                                    else if (c == ']') bracketBalance--;
                                }
                                if (bracketBalance <= 0) {
                                    inBranches = false;
                                    List<String> branches = parseBranchesArray(branchesBuffer.toString());
                                    if (!branches.isEmpty()) chunkConsumer.accept(StreamChunk.branches(branches));
                                    branchesBuffer.setLength(0);
                                }
                            } else {
                                contentBuffer.append(piece);
                                chunkConsumer.accept(StreamChunk.content(piece));
                            }
                        }
                    }
                    JsonNode usage = root.get("usage");
                    if (usage != null && usage.has("total_tokens")) {
                        totalTokens[0] = usage.get("total_tokens").asLong();
                    }
                } catch (Exception e) {
                    log.trace("SSE line parse skip: {}", data, e);
                }
            }
        }

        if (inBranches && branchesBuffer.length() > 0) {
            try {
                List<String> branches = parseBranchesArray(branchesBuffer.toString());
                if (!branches.isEmpty()) chunkConsumer.accept(StreamChunk.branches(branches));
            } catch (Exception ignored) {}
        }

        chunkConsumer.accept(StreamChunk.done());
        return totalTokens[0];
    }

    private static List<String> parseBranchesArray(String raw) {
        List<String> out = new ArrayList<>();
        try {
            JsonNode arr = MAPPER.readTree(raw.trim());
            if (arr.isArray()) {
                for (JsonNode n : arr) {
                    if (n.isTextual()) out.add(n.asText());
                }
            }
        } catch (Exception e) {
            log.trace("Branches parse skip: {}", raw, e);
        }
        return out;
    }
}
