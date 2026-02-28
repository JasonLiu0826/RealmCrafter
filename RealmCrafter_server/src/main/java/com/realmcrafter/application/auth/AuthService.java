package com.realmcrafter.application.auth;

import com.realmcrafter.infrastructure.persistence.entity.UserDO;
import com.realmcrafter.infrastructure.persistence.repository.UserRepository;
import com.realmcrafter.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 注册、登录（账号密码/手机验证码/微信/苹果）、资料更新。
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthResult register(String username, String password, String nickname, String signature) {
        if (userRepository.existsByUsername(username)) {
            return AuthResult.fail("用户名已存在");
        }
        UserDO user = new UserDO();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        if (nickname != null && !nickname.isBlank()) user.setNickname(nickname);
        if (signature != null && !signature.isBlank()) user.setSignature(signature);
        user = userRepository.save(user);
        String token = jwtService.generateToken(user.getId());
        return AuthResult.ok(user.getId(), token, toProfile(user));
    }

    public AuthResult login(String username, String password) {
        UserDO user = userRepository.findByUsername(username).orElse(null);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            return AuthResult.fail("用户名或密码错误");
        }
        if (isSealed(user)) {
            return AuthResult.fail("账号已被封禁");
        }
        String token = jwtService.generateToken(user.getId());
        return AuthResult.ok(user.getId(), token, toProfile(user));
    }

    /** 手机验证码登录：验证码校验为桩实现（开发阶段接受任意 6 位），生产需接入短信服务 */
    @Transactional
    public AuthResult phoneLogin(String phone, String code) {
        if (phone == null || phone.isBlank()) return AuthResult.fail("手机号不能为空");
        // 桩：仅校验 6 位数字
        if (code == null || !code.matches("\\d{6}")) return AuthResult.fail("请输入6位验证码");
        UserDO user = userRepository.findByPhone(phone).orElse(null);
        if (user == null) {
            user = new UserDO();
            user.setUsername("p_" + phone);
            user.setPassword(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
            user.setPhone(phone);
            user = userRepository.save(user);
        }
        if (isSealed(user)) return AuthResult.fail("账号已被封禁");
        String token = jwtService.generateToken(user.getId());
        return AuthResult.ok(user.getId(), token, toProfile(user));
    }

    /** 微信一键登录：根据 OpenID 查或建用户，生产需用 code 换 openid */
    @Transactional
    public AuthResult wechatLogin(String openId) {
        if (openId == null || openId.isBlank()) return AuthResult.fail("openId 不能为空");
        UserDO user = userRepository.findByWechatOpenId(openId).orElse(null);
        if (user == null) {
            user = new UserDO();
            user.setUsername("wx_" + openId.substring(0, Math.min(openId.length(), 50)));
            user.setPassword(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
            user.setWechatOpenId(openId);
            user = userRepository.save(user);
        }
        if (isSealed(user)) return AuthResult.fail("账号已被封禁");
        String token = jwtService.generateToken(user.getId());
        return AuthResult.ok(user.getId(), token, toProfile(user));
    }

    /** 苹果登录：根据 Apple 唯一标识查或建用户，生产需验签 identityToken */
    @Transactional
    public AuthResult appleLogin(String appleId) {
        if (appleId == null || appleId.isBlank()) return AuthResult.fail("appleId 不能为空");
        UserDO user = userRepository.findByAppleId(appleId).orElse(null);
        if (user == null) {
            user = new UserDO();
            user.setUsername("apple_" + appleId.substring(0, Math.min(appleId.length(), 50)));
            user.setPassword(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
            user.setAppleId(appleId);
            user = userRepository.save(user);
        }
        if (isSealed(user)) return AuthResult.fail("账号已被封禁");
        String token = jwtService.generateToken(user.getId());
        return AuthResult.ok(user.getId(), token, toProfile(user));
    }

    public Optional<UserProfileDTO> getProfile(Long userId) {
        return userRepository.findById(userId).map(this::toProfile);
    }

    @Transactional
    public Optional<UserProfileDTO> updateProfile(Long userId, String nickname, String signature, String avatar) {
        return userRepository.findById(userId).map(user -> {
            if (nickname != null) user.setNickname(nickname);
            if (signature != null) user.setSignature(signature);
            if (avatar != null) user.setAvatar(avatar);
            userRepository.save(user);
            return toProfile(user);
        });
    }

    private static boolean isSealed(UserDO user) {
        if (user.getSealedUntil() == null) return false;
        return user.getSealedUntil().isAfter(LocalDateTime.now());
    }

    private UserProfileDTO toProfile(UserDO u) {
        UserProfileDTO dto = new UserProfileDTO();
        dto.setUserId(u.getId());
        dto.setUsername(u.getUsername());
        dto.setRole(u.getRole() != null ? u.getRole().name() : "USER");
        dto.setNickname(u.getNickname());
        dto.setSignature(u.getSignature());
        dto.setAvatar(u.getAvatar());
        dto.setLevel(u.getLevel());
        dto.setExp(u.getExp());
        dto.setIsGoldenCreator(Boolean.TRUE.equals(u.getIsGoldenCreator()));
        dto.setVipExpireTime(u.getVipExpireTime());
        dto.setTokenBalance(u.getTokenBalance());
        dto.setCrystalBalance(u.getCrystalBalance());
        return dto;
    }
}
