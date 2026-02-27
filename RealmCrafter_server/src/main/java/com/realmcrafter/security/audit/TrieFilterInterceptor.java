package com.realmcrafter.security.audit;

import com.realmcrafter.api.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * 第一道防线：本地敏感词前置拦截。
 * 使用字典树（Trie）在请求进入 Controller 前检测请求体，若包含敏感词则直接拒绝。
 */
@Slf4j
@Component
@Order(1)
public class TrieFilterInterceptor implements Filter {

    private static final List<String> DEFAULT_SENSITIVE_WORDS = Arrays.asList(
            "违禁词1", "违禁词2", "敏感词"
            // 生产环境从配置或数据库加载
    );

    private final SensitiveWordTrie trie = new SensitiveWordTrie();

    public TrieFilterInterceptor() {
        for (String word : DEFAULT_SENSITIVE_WORDS) {
            trie.addWord(word);
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if (!shouldCheckBody(httpRequest)) {
            chain.doFilter(request, response);
            return;
        }

        CachingRequestBodyWrapper wrapper;
        try {
            wrapper = new CachingRequestBodyWrapper(httpRequest);
        } catch (IOException e) {
            chain.doFilter(request, response);
            return;
        }

        byte[] bodyBytes = wrapper.getCachedBody();
        if (bodyBytes.length > 0) {
            String body = new String(bodyBytes, StandardCharsets.UTF_8);
            if (trie.containsAny(body)) {
                log.warn("Sensitive word detected in request: uri={}", httpRequest.getRequestURI());

                httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
                httpResponse.setCharacterEncoding(StandardCharsets.UTF_8.name());
                httpResponse.setContentType("application/json;charset=UTF-8");

                Result<Void> result = Result.fail(
                        HttpServletResponse.SC_FORBIDDEN,
                        "检测到高维数据乱流，连接已熔断"
                );
                String json = String.format(
                        "{\"code\":%d,\"message\":\"%s\",\"data\":null}",
                        result.getCode(),
                        result.getMessage()
                );
                httpResponse.getWriter().write(json);
                return;
            }
        }

        chain.doFilter(wrapper, response);
    }

    private boolean shouldCheckBody(HttpServletRequest request) {
        String method = request.getMethod();
        String contentType = request.getContentType();
        if (contentType == null) return false;
        return ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method))
                && (contentType.contains("text/") || contentType.contains("application/json"));
    }
}
