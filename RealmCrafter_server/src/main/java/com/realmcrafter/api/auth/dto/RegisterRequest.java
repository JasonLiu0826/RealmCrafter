package com.realmcrafter.api.auth.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 32)
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64)
    private String password;

    private String nickname;
    private String signature;
}
