package com.realmcrafter.security.audit;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * 可重复读取 Request Body 的包装器：首次读取时缓存字节，后续 getInputStream() 返回基于缓存的流，
 * 供前置敏感词检测与后续 Controller 共用同一 body。
 */
public class CachingRequestBodyWrapper extends HttpServletRequestWrapper {

    private byte[] cachedBody;

    public CachingRequestBodyWrapper(HttpServletRequest request) throws IOException {
        super(request);
        try (InputStream is = request.getInputStream()) {
            cachedBody = is.readAllBytes();
        }
    }

    public byte[] getCachedBody() {
        return cachedBody != null ? Arrays.copyOf(cachedBody, cachedBody.length) : new byte[0];
    }

    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream bais = new ByteArrayInputStream(cachedBody != null ? cachedBody : new byte[0]);
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return bais.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
                throw new UnsupportedOperationException();
            }

            @Override
            public int read() {
                return bais.read();
            }
        };
    }
}
