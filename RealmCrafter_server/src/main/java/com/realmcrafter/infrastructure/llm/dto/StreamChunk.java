package com.realmcrafter.infrastructure.llm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 流式输出片段：content 正文块 或 结束时的 branches 选项。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StreamChunk {

    public enum Type { CONTENT, BRANCHES, DONE }

    private Type type;
    /** content 片段（仅当 type=CONTENT 时有值） */
    private String content;
    /** 选项列表（仅当 type=BRANCHES 时有值） */
    private List<String> branches;

    public static StreamChunk content(String content) {
        return new StreamChunk(Type.CONTENT, content, null);
    }

    public static StreamChunk branches(List<String> branches) {
        return new StreamChunk(Type.BRANCHES, null, branches);
    }

    public static StreamChunk done() {
        return new StreamChunk(Type.DONE, null, null);
    }
}
