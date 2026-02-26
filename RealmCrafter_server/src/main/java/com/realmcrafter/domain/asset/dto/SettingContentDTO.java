package com.realmcrafter.domain.asset.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 设定集核心：五个大维度结构。
 */
@Data
public class SettingContentDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 人物设定 */
    private String characters;

    /** 世界观/故事背景 */
    private String worldview;

    /** 环境场景 */
    private String environment;

    /** 故事主线 */
    private String mainline;

    /** 重要故事要点 */
    private String plotPoints;
}

