package com.realmcrafter.api.asset.dto;

import com.realmcrafter.domain.asset.dto.SettingContentDTO;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
public class UpdateSettingRequest {

    @NotNull
    @Min(1)
    private Long versionId;

    private String title;

    private String cover;

    private String description;

    @NotNull
    private SettingContentDTO content;

    private String deviceHash;
}

