package com.realmcrafter.security;

import com.realmcrafter.infrastructure.persistence.entity.UserDO;
import com.realmcrafter.infrastructure.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 从 Authorization: Bearer &lt;token&gt; 或请求头 X-User-Id 解析当前用户并写入 SecurityContext。
 * 优先 JWT；无 JWT 时若存在 X-User-Id 则暂且放行并注入 SecurityContext（平滑过渡，便于前端未接登录前联调）。
 * 封禁拦截：若用户 sealedUntil 大于当前时间，直接 403「账号封中」。
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        Long userId = null;

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            userId = jwtService.parseUserId(token);
        }
        if (userId == null) {
            String xUserId = request.getHeader("X-User-Id");
            if (xUserId != null && !xUserId.isBlank()) {
                try {
                    userId = Long.parseLong(xUserId.trim());
                } catch (NumberFormatException ignored) {}
            }
        }

        if (userId != null) {
            UserDO user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                if (user.getSealedUntil() != null && user.getSealedUntil().isAfter(LocalDateTime.now())) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"code\":403,\"message\":\"账号封中\"}");
                    return;
                }
                List<SimpleGrantedAuthority> authorities = user.getRole() != null
                        ? Stream.of("ROLE_" + user.getRole().name())
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList())
                        : Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userId, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } else {
                List<SimpleGrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userId, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        chain.doFilter(request, response);
    }
}
