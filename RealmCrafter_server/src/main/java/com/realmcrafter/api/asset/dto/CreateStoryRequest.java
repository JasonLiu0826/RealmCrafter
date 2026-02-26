package com.realmcrafter.api.asset.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

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
}

