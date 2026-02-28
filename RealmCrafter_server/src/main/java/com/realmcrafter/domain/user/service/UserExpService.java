package com.realmcrafter.domain.user.service;

import com.realmcrafter.domain.user.ExpAction;
import com.realmcrafter.domain.social.service.NotificationService;
import com.realmcrafter.infrastructure.persistence.entity.UserDO;
import com.realmcrafter.infrastructure.persistence.entity.UserExpLogDO;
import com.realmcrafter.infrastructure.persistence.repository.UserExpLogRepository;
import com.realmcrafter.infrastructure.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;

/**
 * 经验值与等级流转引擎：加经验、每日防刷、等级跃迁、VIP 跃迁干预。
 */
@Service
@RequiredArgsConstructor
public class UserExpService {

    private static final int MIN_LEVEL = 1;
    private static final int MAX_LEVEL = 5;
    private static final long[] EXP_THRESHOLDS = {0, 500, 2500, 7500, 17500}; // 对应 Lv1~Lv5 下限

    /** 每日行为次数上限，不在此表中的行为不限制次数（吸收认可类与商业行为不设上限） */
    private static final Map<ExpAction, Integer> DAILY_ACTION_CAP = new EnumMap<>(ExpAction.class);

    static {
        DAILY_ACTION_CAP.put(ExpAction.PUBLISH_SETTING, 3);     // 每日最多 3 次（上限 150 EXP）
        DAILY_ACTION_CAP.put(ExpAction.PUBLISH_STORY, 5);       // 每日最多 5 次（上限 150 EXP）
        DAILY_ACTION_CAP.put(ExpAction.READ_CONSUME, 50);       // 每日最多 50 次（上限 100 EXP）
        DAILY_ACTION_CAP.put(ExpAction.FETCH_FROM_SQUARE, 20); // 每日最多 20 次（上限 40 EXP）
        DAILY_ACTION_CAP.put(ExpAction.FORK_ASSET, 10);         // 每日最多 10 次（上限 50 EXP）
        DAILY_ACTION_CAP.put(ExpAction.BE_MENTIONED, 10);       // 每日最多 10 次（上限 20 EXP，防轰炸）
        DAILY_ACTION_CAP.put(ExpAction.PUBLISH_COMMENT, 20);    // 每日最多 20 次
        // BE_LIKED, BE_FAVORITED, BE_SHARED, BE_COMMENTED, BE_FORKED, BE_BOUGHT, VIP_RENEW 不设上限
    }

    private final UserRepository userRepository;
    private final UserExpLogRepository userExpLogRepository;
    private final NotificationService notificationService;

    /**
     * 为用户增加经验。先做每日防刷校验，通过则写流水并更新 UserDO.exp/level；超限则静默返回不抛异常。
     * VIP_RENEW 时若等级 &lt; 2 会强制跃迁至 Lv2。
     * 使用 REQUIRES_NEW 以便在只读事务（如广场列表）中调用时仍能正确写入。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void addExp(Long userId, ExpAction action) {
        if (userId == null || action == null) return;

        UserDO user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        LocalDate today = LocalDate.now();
        String actionType = action.name();
        Integer cap = DAILY_ACTION_CAP.get(action);
        if (cap != null) {
            long count = userExpLogRepository.countByUserIdAndCreateDateAndActionType(userId, today, actionType);
            if (count >= cap) return;
        }

        long expGained = action.getExp();
        UserExpLogDO log = new UserExpLogDO();
        log.setUserId(userId);
        log.setActionType(actionType);
        log.setExpGained(expGained);
        log.setCreateDate(today);
        log.setCreateTime(LocalDateTime.now());
        userExpLogRepository.save(log);

        long newExp = (user.getExp() != null ? user.getExp() : 0L) + expGained;

        if (action == ExpAction.VIP_RENEW && (user.getLevel() == null || user.getLevel() < 2)) {
            newExp = Math.max(newExp, 500L);
            user.setExp(newExp);
            user.setLevel(2);
            userRepository.save(user);
            notificationService.sendLevelUp(userId, 2);
            return;
        }

        user.setExp(newExp);
        int newLevel = expToLevel(newExp);
        int oldLevel = user.getLevel() != null ? Math.min(MAX_LEVEL, Math.max(MIN_LEVEL, user.getLevel())) : MIN_LEVEL;
        user.setLevel(newLevel);
        userRepository.save(user);

        if (newLevel > oldLevel) {
            notificationService.sendLevelUp(userId, newLevel);
        }
    }

    /**
     * 经验值 -> 等级：&lt;500 Lv1, 500-2499 Lv2, 2500-7499 Lv3, 7500-17499 Lv4, &gt;=17500 Lv5。
     */
    public static int expToLevel(long exp) {
        for (int i = EXP_THRESHOLDS.length - 1; i >= 0; i--) {
            if (exp >= EXP_THRESHOLDS[i]) return i + 1;
        }
        return MIN_LEVEL;
    }
}
