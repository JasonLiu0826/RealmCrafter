package com.realmcrafter.domain.user;

import lombok.Getter;

/**
 * 行为类型与单次经验值映射（见经验值配置与防刷上限平衡表）。
 */
@Getter
public enum ExpAction {

    // ---------- 活跃创作与消费类（有严格每日上限） ----------
    PUBLISH_SETTING(50),
    PUBLISH_STORY(30),
    READ_CONSUME(2),
    FETCH_FROM_SQUARE(2),
    FORK_ASSET(5),

    // ---------- 主动社交互动类 ----------
    BE_MENTIONED(2),
    PUBLISH_COMMENT(2),

    // ---------- 吸收认可类（价值高，无上限或高上限） ----------
    BE_LIKED(3),
    BE_FAVORITED(5),
    BE_SHARED(10),
    BE_COMMENTED(5),
    BE_FORKED(15),
    BE_BOUGHT(50),

    // ---------- 商业行为类 ----------
    VIP_RENEW(500);

    private final long exp;

    ExpAction(long exp) {
        this.exp = exp;
    }
}
