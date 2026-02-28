package com.realmcrafter.application.chapter;

import com.realmcrafter.domain.billing.InsufficientTokenException;
import com.realmcrafter.domain.billing.strategy.BillingStrategy;
import com.realmcrafter.domain.chapter.PurificationEngine;
import com.realmcrafter.domain.chapter.service.ChapterGenerationService;
import com.realmcrafter.infrastructure.llm.LlmClient;
import com.realmcrafter.infrastructure.llm.dto.LlmStreamRequest;
import com.realmcrafter.infrastructure.llm.dto.StreamChunk;
import com.realmcrafter.infrastructure.persistence.entity.ChapterDO;
import com.realmcrafter.infrastructure.persistence.entity.StoryDO;
import com.realmcrafter.infrastructure.persistence.entity.UserDO;
import com.realmcrafter.infrastructure.persistence.repository.ChapterRepository;
import com.realmcrafter.infrastructure.persistence.repository.SettingPackRepository;
import com.realmcrafter.infrastructure.persistence.repository.StoryRepository;
import com.realmcrafter.infrastructure.persistence.repository.UserRepository;
import com.realmcrafter.infrastructure.redis.StoryGenerationLock;
import com.realmcrafter.infrastructure.vector.VectorMemoryService;
import com.realmcrafter.security.audit.SensitiveWordTrie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

/**
 * 章节流式生成应用服务：前置风控、鉴权、预扣、加锁、上下文拼装、LLM 调度、净化、持久化与结算。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChapterApplicationService {

    private final SensitiveWordTrie sensitiveWordTrie;
    private final StoryRepository storyRepository;
    private final SettingPackRepository settingPackRepository;
    private final ChapterRepository chapterRepository;
    private final UserRepository userRepository;
    private final ChapterGenerationService chapterGenerationService;
    private final LlmClient llmClient;
    private final StoryGenerationLock storyGenerationLock;
    private final List<BillingStrategy> billingStrategies;
    private final VectorMemoryService vectorMemoryService;

    /**
     * 流式生成一章：前置校验 → 加锁 → 计费预扣 → 拼装 L1+L2 → 调用 LLM 流式输出 → 净化 → 回调 chunkConsumer（含 content/branches/done）。
     * 若 userChoice 含敏感词则抛 IllegalArgumentException；Token 不足抛 InsufficientTokenException。
     */
    @Transactional
    public void generateStream(String storyId, Long userId, String userChoice, double chaosLevel, boolean useByok,
                              Consumer<StreamChunk> chunkConsumer) throws Exception {
        if (PurificationEngine.hasMemeticHazard(userChoice) || sensitiveWordTrie.containsAny(userChoice)) {
            throw new IllegalArgumentException("输入包含违规内容，请修改后重试");
        }

        StoryDO story = storyRepository.findById(storyId)
                .orElseThrow(() -> new IllegalArgumentException("故事不存在"));
        if (!story.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权操作该故事");
        }

        if (!storyGenerationLock.tryLock(storyId)) {
            throw new IllegalStateException("该故事正在生成中，请稍后再试");
        }

        try {
            UserDO user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
            BillingStrategy strategy = resolveStrategy(user, useByok);
            strategy.beforeChapterGeneration(user);
            userRepository.save(user);

            var settingPack = settingPackRepository.findById(story.getSettingPackId())
                    .orElseThrow(() -> new IllegalArgumentException("关联设定集不存在"));
            List<ChapterDO> recent = chapterRepository.findTop5ByStoryIdOrderByChapterIndexDesc(storyId);
            String userIntent = (userChoice != null && !userChoice.isBlank()) ? userChoice : "";
            String systemPrompt = chapterGenerationService.assembleSystemPrompt(settingPack, recent, storyId, userIntent);

            String userMessage = (userChoice != null && !userChoice.isBlank())
                    ? userChoice
                    : "请根据当前剧情继续发展。";

            LlmStreamRequest request = LlmStreamRequest.builder()
                    .systemPrompt(systemPrompt)
                    .userMessage(userMessage)
                    .temperature(chaosLevel)
                    .apiKey(useByok ? resolveUserApiKey(userId) : null)
                    .build();

            StringBuilder fullContent = new StringBuilder();
            List<String> branches = new java.util.ArrayList<>();

            Consumer<StreamChunk> inner = chunk -> {
                if (chunk.getType() == StreamChunk.Type.CONTENT && chunk.getContent() != null) {
                    String cleaned = PurificationEngine.cleanStreamText(chunk.getContent());
                    fullContent.append(cleaned);
                    chunkConsumer.accept(StreamChunk.content(cleaned));
                } else if (chunk.getType() == StreamChunk.Type.BRANCHES && chunk.getBranches() != null) {
                    branches.addAll(chunk.getBranches());
                    chunkConsumer.accept(chunk);
                } else if (chunk.getType() == StreamChunk.Type.DONE) {
                    chunkConsumer.accept(StreamChunk.done());
                }
            };

            Long totalTokens = llmClient.stream(request, inner);

            int nextIndex = (story.getLastChapterIndex() != null ? story.getLastChapterIndex() : 0) + 1;
            ChapterDO chapter = new ChapterDO();
            chapter.setStoryId(storyId);
            chapter.setChapterIndex(nextIndex);
            chapter.setTitle("第" + nextIndex + "章");
            chapter.setContent(fullContent.toString());
            String branchesJson = "[]";
            if (!branches.isEmpty()) {
                try {
                    branchesJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(branches);
                } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                    branchesJson = "[]";
                }
            }
            chapter.setBranchesData(branchesJson);
            chapterRepository.save(chapter);

            story.setLastChapterIndex(nextIndex);
            storyRepository.save(story);

            vectorMemoryService.indexChapterChunks(storyId, chapter.getId() != null ? chapter.getId().longValue() : nextIndex, chapter.getContent());

            // 当前阶段使用预扣（beforeChapterGeneration）；后续可改为按 totalTokens 实际结算
        } finally {
            storyGenerationLock.unlock(storyId);
        }
    }

    private BillingStrategy resolveStrategy(UserDO user, boolean useByok) {
        boolean byok = useByok || Boolean.TRUE.equals(user.getIsByok());
        return billingStrategies.stream()
                .filter(s -> s.isByok() == byok)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("未找到匹配的计费策略"));
    }

    /** BYOK 时从用户配置读取 apiKey，暂无字段时返回 null（后续可扩展 user_config.api_key）。 */
    private String resolveUserApiKey(Long userId) {
        return null;
    }
}
