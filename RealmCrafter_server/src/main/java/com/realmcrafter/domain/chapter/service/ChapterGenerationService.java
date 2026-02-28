package com.realmcrafter.domain.chapter.service;

import com.realmcrafter.domain.asset.dto.SettingContentDTO;
import com.realmcrafter.infrastructure.persistence.entity.ChapterDO;
import com.realmcrafter.infrastructure.persistence.entity.SettingPackDO;
import com.realmcrafter.infrastructure.vector.VectorMemoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 章节生成领域服务：三级混合记忆 L1（设定集）、L2（短期滑动窗口）、L3（RAG 回忆碎片）拼装。
 */
@Service
@RequiredArgsConstructor
public class ChapterGenerationService {

    @Value("${realmcrafter.engine.l2-window-size:5}")
    private int l2WindowSize = 5;

    @Value("${realmcrafter.engine.l3-top-k:3}")
    private int l3TopK = 3;

    private final VectorMemoryService vectorMemoryService;

    private static final String L1_TEMPLATE =
            "你是一位严谨的互动小说引擎。必须严格遵守以下设定与世界观，不得偏离。\n\n"
                    + "【人物设定】\n%s\n\n【世界观/故事背景】\n%s\n\n【环境场景】\n%s\n\n【故事主线】\n%s\n\n【重要情节要点】\n%s\n\n"
                    + "请根据用户的选择推进剧情，输出正文与分支。";

    /** 输出格式绝对指令：强制恰好 3 个分支、纯 JSON、无 Markdown，降低格式错误率。 */
    private static final String OUTPUT_FORMAT_INSTRUCTION =
            "\n\n【输出格式绝对指令】\n"
                    + "你必须且只能提供恰好 3 个后续剧情分支选项，代表主角接下来可能采取的三种不同行动（如：激进、保守、探索）。\n"
                    + "你必须严格以 JSON 格式输出，不可包含任何 Markdown 代码块标签(如 ```json)。\n"
                    + "JSON 结构必须完全一致：\n"
                    + "{\n"
                    + "  \"content\": \"这里是引人入胜的故事正文...\",\n"
                    + "  \"branches\": [\"分支选项A\", \"分支选项B\", \"分支选项C\"]\n"
                    + "}";

    /**
     * 构建发往 LLM 的系统提示（L1：设定集五大维度）。
     */
    public String buildL1SystemPrompt(SettingPackDO settingPack) {
        SettingContentDTO c = settingPack.getContent();
        if (c == null) {
            c = new SettingContentDTO();
        }
        String characters = nullToEmpty(c.getCharacters());
        String worldview = nullToEmpty(c.getWorldview());
        String environment = nullToEmpty(c.getEnvironment());
        String mainline = nullToEmpty(c.getMainline());
        String plotPoints = nullToEmpty(c.getPlotPoints());
        return String.format(L1_TEMPLATE, characters, worldview, environment, mainline, plotPoints);
    }

    /**
     * 构建 L2 短期记忆：最近 N 章完整内容，按时间正序拼接。
     */
    public String buildL2Context(List<ChapterDO> recentChapters) {
        if (recentChapters == null || recentChapters.isEmpty()) return "";
        // 取最新 N 章且按 chapter_index 升序
        List<ChapterDO> ordered = recentChapters.stream()
                .sorted((a, b) -> Integer.compare(a.getChapterIndex(), b.getChapterIndex()))
                .limit(l2WindowSize)
                .collect(Collectors.toList());
        StringBuilder sb = new StringBuilder();
        sb.append("\n\n【近期剧情（请保持连贯）】\n");
        for (ChapterDO ch : ordered) {
            sb.append("--- 第").append(ch.getChapterIndex()).append("章");
            if (ch.getTitle() != null && !ch.getTitle().isEmpty()) {
                sb.append(" ").append(ch.getTitle());
            }
            sb.append(" ---\n").append(ch.getContent()).append("\n");
        }
        return sb.toString();
    }

    /**
     * 构建 L3 潜意识记忆：从向量库检索与用户意图最相关的历史碎片。
     */
    public String buildL3Context(String storyId, String userIntent) {
        List<String> fragments = vectorMemoryService.searchRelevantFragments(storyId, userIntent, l3TopK);
        if (fragments == null || fragments.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("\n\n【历史回忆碎片（可呼应伏笔）】\n");
        for (int i = 0; i < fragments.size(); i++) {
            sb.append("— 碎片").append(i + 1).append(" —\n").append(fragments.get(i)).append("\n");
        }
        return sb.toString();
    }

    /**
     * 合并 L1 + L2 + L3 + 输出格式绝对指令为最终系统提示。
     */
    public String assembleSystemPrompt(SettingPackDO settingPack, List<ChapterDO> recentChapters,
                                       String storyId, String userIntent) {
        String l1 = buildL1SystemPrompt(settingPack);
        String l2 = buildL2Context(recentChapters);
        String l3 = buildL3Context(storyId, userIntent);
        return l1 + l2 + l3 + OUTPUT_FORMAT_INSTRUCTION;
    }

    /**
     * 仅 L1 + L2（兼容旧调用，无 L3）。
     */
    public String assembleSystemPrompt(SettingPackDO settingPack, List<ChapterDO> recentChapters) {
        return assembleSystemPrompt(settingPack, recentChapters, null, null);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
