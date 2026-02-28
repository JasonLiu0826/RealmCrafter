package com.realmcrafter.api.asset.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class CreateStoryRequest {

    @NotNull
    private Long userId;

    @NotBlank
    private String settingPackId;

    @NotBlank
    private String title;

    private String cover;

    private String description;

    /** 可选定价，受创作者等级上限约束 */
    private BigDecimal price;
}

