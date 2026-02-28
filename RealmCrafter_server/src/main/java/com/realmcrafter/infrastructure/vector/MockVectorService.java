package com.realmcrafter.infrastructure.vector;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Mock 向量记忆：内存存储，为以后接入 Milvus 做准备。
 * 按 200 字切片存储；检索时按关键词简单匹配模拟 Top-K 相似度。
 */
@Slf4j
@Service
public class MockVectorService implements VectorMemoryService {

    private static final int CHUNK_SIZE = 200;

    @Value("${realmcrafter.engine.l3-top-k:3}")
    private int defaultTopK = 3;

    /** storyId -> list of chunk texts (order preserved) */
    private final Map<String, List<String>> storyChunks = new ConcurrentHashMap<>();

    @Override
    public void indexChapterChunks(String storyId, long chapterId, String content) {
        if (content == null || content.isEmpty()) return;
        List<String> chunks = storyChunks.computeIfAbsent(storyId, k -> new ArrayList<>());
        for (int i = 0; i < content.length(); i += CHUNK_SIZE) {
            int end = Math.min(i + CHUNK_SIZE, content.length());
            chunks.add(content.substring(i, end));
        }
        log.debug("MockVectorService indexed storyId={} chapterId={} chunks={}", storyId, chapterId, chunks.size());
    }

    @Override
    public List<String> searchRelevantFragments(String storyId, String userIntent, int topK) {
        List<String> chunks = storyChunks.get(storyId);
        if (chunks == null || chunks.isEmpty() || userIntent == null || userIntent.isBlank()) {
            return Collections.emptyList();
        }
        String intent = userIntent.trim().toLowerCase();
        if (intent.isEmpty()) return Collections.emptyList();

        List<String> scored = chunks.stream()
                .map(chunk -> {
                    String lower = chunk.toLowerCase();
                    long score = Arrays.stream(intent.split("\\s+"))
                            .filter(word -> word.length() > 1 && lower.contains(word))
                            .count();
                    return new AbstractMap.SimpleEntry<>(chunk, score);
                })
                .filter(e -> e.getValue() > 0)
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(topK)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (scored.isEmpty() && !chunks.isEmpty()) {
            int from = Math.max(0, chunks.size() - topK);
            return new ArrayList<>(chunks.subList(from, chunks.size()));
        }
        return scored;
    }
}
