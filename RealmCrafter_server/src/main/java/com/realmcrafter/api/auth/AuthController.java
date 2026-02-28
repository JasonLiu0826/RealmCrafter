package com.realmcrafter.api.auth;

import com.realmcrafter.api.dto.Result;
import com.realmcrafter.api.auth.dto.*;
import com.realmcrafter.application.auth.AuthResult;
import com.realmcrafter.application.auth.AuthService;
import com.realmcrafter.application.auth.UserProfileDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

/**
 * 鉴权与用户资料：注册、登录（账号密码/手机/微信/苹果）、获取/更新资料。
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<Result<Map<String, Object>>> register(@RequestBody @Valid RegisterRequest request) {
        AuthResult r = authService.register(
                request.getUsername(),
                request.getPassword(),
                request.getNickname(),
                request.getSignature()
        );
        if (!r.isSuccess()) {
            return ResponseEntity.ok(Result.fail(400, r.getMessage()));
        }
        return ResponseEntity.ok(Result.ok(Map.of(
                "userId", r.getUserId(),
                "token", r.getToken(),
                "profile", r.getProfile()
        )));
    }

    @PostMapping("/login")
    public ResponseEntity<Result<Map<String, Object>>> login(@RequestBody @Valid LoginRequest request) {
        AuthResult r = authService.login(request.getUsername(), request.getPassword());
        if (!r.isSuccess()) {
            return ResponseEntity.ok(Result.fail(401, r.getMessage()));
        }
        return ResponseEntity.ok(Result.ok(Map.of(
                "userId", r.getUserId(),
                "token", r.getToken(),
                "profile", r.getProfile()
        )));
    }

    @PostMapping("/phone-login")
    public ResponseEntity<Result<Map<String, Object>>> phoneLogin(@RequestBody @Valid PhoneLoginRequest request) {
        AuthResult r = authService.phoneLogin(request.getPhone(), request.getCode());
        if (!r.isSuccess()) {
            return ResponseEntity.ok(Result.fail(401, r.getMessage()));
        }
        return ResponseEntity.ok(Result.ok(Map.of(
                "userId", r.getUserId(),
                "token", r.getToken(),
                "profile", r.getProfile()
        )));
    }

    @PostMapping("/wechat-login")
    public ResponseEntity<Result<Map<String, Object>>> wechatLogin(@RequestBody @Valid WechatLoginRequest request) {
        AuthResult r = authService.wechatLogin(request.getOpenId());
        if (!r.isSuccess()) {
            return ResponseEntity.ok(Result.fail(401, r.getMessage()));
        }
        return ResponseEntity.ok(Result.ok(Map.of(
                "userId", r.getUserId(),
                "token", r.getToken(),
                "profile", r.getProfile()
        )));
    }

    @PostMapping("/apple-login")
    public ResponseEntity<Result<Map<String, Object>>> appleLogin(@RequestBody @Valid AppleLoginRequest request) {
        AuthResult r = authService.appleLogin(request.getAppleId());
        if (!r.isSuccess()) {
            return ResponseEntity.ok(Result.fail(401, r.getMessage()));
        }
        return ResponseEntity.ok(Result.ok(Map.of(
                "userId", r.getUserId(),
                "token", r.getToken(),
                "profile", r.getProfile()
        )));
    }

    @GetMapping("/profile")
    public Result<UserProfileDTO> getProfile() {
        Long userId = resolveCurrentUserId();
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        return authService.getProfile(userId)
                .map(Result::ok)
                .orElseGet(() -> Result.fail(404, "用户不存在"));
    }

    @PatchMapping("/profile")
    public Result<UserProfileDTO> updateProfile(@RequestBody @Valid UpdateProfileRequest request) {
        Long userId = resolveCurrentUserId();
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        return authService.updateProfile(
                userId,
                request.getNickname(),
                request.getSignature(),
                request.getAvatar()
        ).map(Result::ok).orElseGet(() -> Result.fail(404, "用户不存在"));
    }

    private static Long resolveCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) return null;
        Object principal = auth.getPrincipal();
        if (principal instanceof Long) return (Long) principal;
        if (principal instanceof String) {
            try { return Long.parseLong((String) principal); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }
}
