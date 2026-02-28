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

    /** 允许克隆下载，默认 true */
    private Boolean allowDownload = true;

    /** 允许二次修改（Fork 副本是否可更新），默认 true */
    private Boolean allowModify = true;
}

