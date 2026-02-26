package com.realmcrafter.api.asset.dto;

import com.realmcrafter.domain.asset.dto.SettingContentDTO;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class CreateSettingRequest {

    @NotBlank
    private String title;

    private String cover;

    private String description;

    @NotNull
    private SettingContentDTO content;

    private String deviceHash;
}

