package com.realmcrafter.api.auth.dto;

import lombok.Data;

import javax.validation.constraints.Size;

@Data
public class UpdateProfileRequest {

    @Size(max = 64)
    private String nickname;

    @Size(max = 256)
    private String signature;

    @Size(max = 512)
    private String avatar;
}
