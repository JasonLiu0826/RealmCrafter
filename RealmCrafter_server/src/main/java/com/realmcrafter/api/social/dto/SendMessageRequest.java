package com.realmcrafter.api.social.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 发送私信请求。
 */
@Data
public class SendMessageRequest {

    @NotNull(message = "接收者 ID 不能为空")
    private Long receiverId;

    /** TEXT | FORWARD_CARD，默认 TEXT */
    private String msgType = "TEXT";

    @NotBlank(message = "消息内容不能为空")
    private String content;
}
