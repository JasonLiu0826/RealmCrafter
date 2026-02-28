package com.realmcrafter.domain.chapter.service;

import com.realmcrafter.domain.asset.dto.SettingContentDTO;
import com.realmcrafter.infrastructure.persistence.entity.ChapterDO;
import com.realmcrafter.infrastructure.persistence.entity.SettingPackDO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 章节生成领域服务：三级混合记忆中的 L1（绝对记忆）与 L2（短期滑动窗口）拼装。
 * L3 RAG 暂不实现，由后续扩展。
 */
@Service
@RequiredArgsConstructor
public class ChapterGenerationService {

    @Value("${realmcrafter.engine.l2-window-size:5}")
    private int l2WindowSize = 5;

    private static final String L1_TEMPLATE =
            "你是一位严谨的互动小说引擎。必须严格遵守以下设定与世界观，不得偏离。\n\n"
                    + "【人物设定】\n%s\n\n【世界观/故事背景】\n%s\n\n【环境场景】\n%s\n\n【故事主线】\n%s\n\n【重要情节要点】\n%s\n\n"
                    + "请根据用户的选择推进剧情，输出正文。正文结束后另起一行输出：BRANCHES:\n[\"选项A\",\"选项B\",\"选项C\"]，提供 3 个后续选项。";

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
     * 合并 L1 + L2 为最终系统提示。
     */
    public String assembleSystemPrompt(SettingPackDO settingPack, List<ChapterDO> recentChapters) {
        String l1 = buildL1SystemPrompt(settingPack);
        String l2 = buildL2Context(recentChapters);
        return l1 + l2;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
