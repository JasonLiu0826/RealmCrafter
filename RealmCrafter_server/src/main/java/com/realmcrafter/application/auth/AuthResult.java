package com.realmcrafter.application.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录/注册统一返回：token + 用户资料。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResult {

    private boolean success;
    private String message;
    private Long userId;
    private String token;
    private UserProfileDTO profile;

    public static AuthResult ok(Long userId, String token, UserProfileDTO profile) {
        return new AuthResult(true, null, userId, token, profile);
    }

    public static AuthResult fail(String message) {
        return new AuthResult(false, message, null, null, null);
    }
}
