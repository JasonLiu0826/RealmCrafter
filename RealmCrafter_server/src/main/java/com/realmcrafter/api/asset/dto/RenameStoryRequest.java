package com.realmcrafter.api.asset.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class RenameStoryRequest {

    @NotNull
    private Long userId;

    @NotBlank
    private String title;
}

