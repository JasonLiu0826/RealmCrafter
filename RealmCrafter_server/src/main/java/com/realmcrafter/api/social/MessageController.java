package com.realmcrafter.api.social;

import com.realmcrafter.api.dto.Result;
import com.realmcrafter.api.social.dto.SendMessageRequest;
import com.realmcrafter.domain.social.service.MessageService;
import com.realmcrafter.infrastructure.persistence.entity.MessageDO;
import com.realmcrafter.infrastructure.persistence.entity.MessageSessionDO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 私信 IM：发送消息、会话列表、聊天记录。
 * 需登录，请求头 X-User-Id 传递当前用户。
 */
@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
@Validated
public class MessageController {

    private final MessageService messageService;

    /** 发送私信 */
    @PostMapping("/send")
    public Result<MessageDO> send(@RequestHeader("X-User-Id") Long senderId,
                                  @RequestBody @Valid SendMessageRequest request) {
        MessageDO.MsgType type = parseMsgType(request.getMsgType());
        MessageDO msg = messageService.sendMessage(senderId, request.getReceiverId(), type, request.getContent());
        return Result.ok(msg);
    }

    /** 获取最近会话列表（含最后一条消息摘要与未读数） */
    @GetMapping("/sessions")
    public Result<Page<MessageSessionDO>> sessions(@RequestHeader("X-User-Id") Long userId,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "lastMessageAt"));
        return Result.ok(messageService.getRecentSessions(userId, pageable));
    }

    /** 获取与某人的聊天记录（按时间倒序） */
    @GetMapping("/chat/{peerId}")
    public Result<Page<MessageDO>> chat(@RequestHeader("X-User-Id") Long userId,
                                        @PathVariable Long peerId,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
        return Result.ok(messageService.getChatHistory(userId, peerId, pageable));
    }

    private static MessageDO.MsgType parseMsgType(String msgType) {
        if (msgType == null || msgType.isBlank()) return MessageDO.MsgType.TEXT;
        if ("FORWARD_CARD".equalsIgnoreCase(msgType.trim())) return MessageDO.MsgType.FORWARD_CARD;
        return MessageDO.MsgType.TEXT;
    }
}
