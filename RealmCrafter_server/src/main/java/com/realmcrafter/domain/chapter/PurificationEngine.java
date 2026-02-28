package com.realmcrafter.domain.chapter;

import java.util.regex.Pattern;

/**
 * 核心净化引擎：剥离大模型的“说教感”与“元数据”，保证沉浸感。
 * 在打字机渲染前对流式正文调用 cleanStreamText；发往大模型前可调用 hasMemeticHazard 做前置阻断。
 */
public final class PurificationEngine {

    /** 拦截【上帝视角】【旁白】等解析标签 */
    private static final Pattern META_REGEX = Pattern.compile(
            "【[^】]*(视角|描写|转场|切换|解析|心理|旁白|分析|画外音|特写|镜头|提示|说明|注意)[^】]*】",
            Pattern.UNICODE_CHARACTER_CLASS);

    /** 拦截破除第四面墙的开头语 */
    private static final Pattern CHAT_REGEX = Pattern.compile(
            "^(好的，|没问题，|为您生成|接下来|在这段剧情中).*?(。|：|\n)",
            Pattern.UNICODE_CHARACTER_CLASS | Pattern.MULTILINE);

    private PurificationEngine() {}

    /**
     * 实时流式净化（在打字机渲染前调用）。
     *
     * @param rawText 原始流式字符串
     * @return 净化后的字符串
     */
    public static String cleanStreamText(String rawText) {
        if (rawText == null || rawText.isEmpty()) return "";
        String clean = META_REGEX.matcher(rawText).replaceAll("");
        clean = CHAT_REGEX.matcher(clean).replaceAll("");
        return clean.trim();
    }

    /**
     * 前置词库阻断（发往大模型前调用，节省 Token 且防封号）。
     * 实际企业级可注入 SensitiveWordTrie 或云端敏感词库。
     *
     * @param input 用户输入/选择
     * @return 若包含违禁词返回 true
     */
    public static boolean hasMemeticHazard(String input) {
        if (input == null || input.isBlank()) return false;
        // 占位：可替换为 SensitiveWordTrie.containsAny(input)
        return false;
    }
}
