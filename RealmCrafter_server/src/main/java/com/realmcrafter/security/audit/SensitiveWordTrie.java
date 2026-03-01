package com.realmcrafter.security.audit;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 敏感词字典树（Trie），用于快速检测文本中是否包含敏感词。
 * 第一道防线：本地敏感词前置拦截。
 */
@Component
public class SensitiveWordTrie {

    private final TrieNode root = new TrieNode();

    public static class TrieNode {
        private final Map<Character, TrieNode> children = new HashMap<>();
        private boolean end;

        public TrieNode getChild(char c) {
            return children.get(c);
        }

        public TrieNode getOrPutChild(char c) {
            return children.computeIfAbsent(c, k -> new TrieNode());
        }

        public boolean isEnd() {
            return end;
        }

        public void setEnd(boolean end) {
            this.end = end;
        }
    }

    /** 添加一个敏感词 */
    public void addWord(String word) {
        if (word == null || word.isEmpty()) return;
        TrieNode node = root;
        for (int i = 0; i < word.length(); i++) {
            char c = Character.toLowerCase(word.charAt(i));
            node = node.getOrPutChild(c);
        }
        node.setEnd(true);
    }

    /**
     * 检查文本中是否包含任意已添加的敏感词。
     *
     * @param text 待检测文本
     * @return 若包含敏感词返回 true
     */
    public boolean containsAny(String text) {
        if (text == null || text.isEmpty()) return false;
        String lower = text.toLowerCase();
        for (int i = 0; i < lower.length(); i++) {
            TrieNode node = root;
            for (int j = i; j < lower.length(); j++) {
                node = node.getChild(lower.charAt(j));
                if (node == null) break;
                if (node.isEnd()) return true;
            }
        }
        return false;
    }

    /**
     * 将敏感词替换为占位符。
     *
     * @param text    原文
     * @param replace 替换字符串（如 ***）
     * @return 替换后的文本
     */
    public String replaceSensitiveWords(String text, String replace) {
        if (text == null || text.isEmpty()) return text;
        String lower = text.toLowerCase();
        StringBuilder sb = new StringBuilder(text);
        for (int i = 0; i < lower.length(); i++) {
            TrieNode node = root;
            int endIdx = -1;
            for (int j = i; j < lower.length(); j++) {
                node = node.getChild(lower.charAt(j));
                if (node == null) break;
                if (node.isEnd()) endIdx = j;
            }
            if (endIdx >= 0) {
                for (int k = i; k <= endIdx; k++) {
                    sb.setCharAt(k, replace.charAt(0));
                }
                i = endIdx;
            }
        }
        return sb.toString().replaceAll("\\*+", replace);
    }
}
