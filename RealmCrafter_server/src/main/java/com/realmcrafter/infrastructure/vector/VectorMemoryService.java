package com.realmcrafter.infrastructure.vector;

import java.util.List;

/**
 * 向量记忆服务抽象：为后续接入 Milvus / pgvector 做准备。
 * 存储：按 200 字片将章节内容转为 Embedding 写入；
 * 回忆：按故事 + 用户意图检索最相关 Top-K 条历史碎片。
 */
public interface VectorMemoryService {

    /**
     * 将新章节内容按 200 字切片写入向量库（或 Mock 存储）。
     *
     * @param storyId  故事 ID
     * @param chapterId 章节 ID（或序号）
     * @param content   章节正文
     */
    void indexChapterChunks(String storyId, long chapterId, String content);

    /**
     * 根据用户最新选项的“意图”在故事下检索最相关的若干条历史碎片。
     *
     * @param storyId    故事 ID
     * @param userIntent 用户选择/输入（意图）
     * @param topK       返回条数，如 3
     * @return 历史回忆片段文本列表
     */
    List<String> searchRelevantFragments(String storyId, String userIntent, int topK);
}
