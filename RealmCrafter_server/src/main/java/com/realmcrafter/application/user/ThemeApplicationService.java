package com.realmcrafter.application.user;

import com.realmcrafter.domain.billing.VipAccessDeniedException;
import com.realmcrafter.domain.billing.service.VipValidator;
import com.realmcrafter.infrastructure.persistence.entity.UserDO;
import com.realmcrafter.infrastructure.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 用户主题偏好相关的应用服务。
 *
 * 组合：查用户 -> 校验主题合法性 -> VIP 校验 -> 持久化 currentThemeId。
 */
@Service
@RequiredArgsConstructor
public class ThemeApplicationService {

    private final UserRepository userRepository;
    private final VipValidator vipValidator;

    /**
     * 所有合法的主题 ID 列表。
     *
     * 与前端 src/assets/themes/index.ts 中的 ThemeId 保持一致。
     */
    private static final Set<String> ALL_THEME_IDS;

    /**
     * 需要 VIP 权限的主题 ID 列表。
     */
    private static final Set<String> VIP_THEME_IDS;

    static {
        Set<String> all = new HashSet<>();
        // 普通主题
        all.add("classic_white");
        all.add("dark_night");
        all.add("paper");
        // VIP 主题
        all.add("sakura_pink");
        all.add("cyber_purple");
        all.add("hacker_green");
        all.add("tech_blue");
        all.add("rose_red");
        all.add("sunset_orange");
        all.add("cyber_gradient");
        ALL_THEME_IDS = Collections.unmodifiableSet(all);

        Set<String> vip = new HashSet<>();
        vip.add("sakura_pink");
        vip.add("cyber_purple");
        vip.add("hacker_green");
        vip.add("tech_blue");
        vip.add("rose_red");
        vip.add("sunset_orange");
        vip.add("cyber_gradient");
        VIP_THEME_IDS = Collections.unmodifiableSet(vip);
    }

    /**
     * 更新当前登录用户的主题偏好。
     *
     * @param userId  当前用户 ID
     * @param themeId 目标主题 ID
     */
    @Transactional
    public void updateUserTheme(Long userId, String themeId) {
        if (!ALL_THEME_IDS.contains(themeId)) {
            throw new IllegalArgumentException("非法的主题 ID");
        }

        UserDO user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        // VIP 主题需要校验 vipExpireTime
        if (VIP_THEME_IDS.contains(themeId) && !vipValidator.hasActiveVip(user.getVipExpireTime())) {
            throw new VipAccessDeniedException("请开通 VIP 后使用此主题");
        }

        user.setCurrentThemeId(themeId);
        userRepository.save(user);
    }
}

