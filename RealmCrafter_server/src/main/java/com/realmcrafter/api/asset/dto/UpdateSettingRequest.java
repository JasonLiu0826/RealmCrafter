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

    /** 允许克隆下载（仅原创设定可修改） */
    private Boolean allowDownload;

    /** 允许二次修改（仅原创设定可修改） */
    private Boolean allowModify;
}

