package com.realmcrafter.api.user.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

@Data
public class UpdateEngineConfigRequest {

    /**
     * 混沌阈值，建议范围 0.1 - 1.0。
     */
    @Min(0)
    @Max(2)
    private Double chaosLevel;

    /**
     * 首选模型名称。
     */
    private String preferredModel;
}

