package com.realmcrafter.domain.social.service;

import com.realmcrafter.infrastructure.persistence.entity.MessageDO;
import com.realmcrafter.infrastructure.persistence.entity.MessageSessionDO;
import com.realmcrafter.infrastructure.persistence.repository.MessageRepository;
import com.realmcrafter.infrastructure.persistence.repository.MessageSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 私信 IM：发送消息、拉取聊天记录、最近会话列表。
 */
@Service
@RequiredArgsConstructor
public class MessageService {

    private static final int PREVIEW_MAX_LEN = 80;

    private final MessageRepository messageRepository;
    private final MessageSessionRepository messageSessionRepository;

    @Transactional
    public MessageDO sendMessage(Long senderId, Long receiverId, MessageDO.MsgType type, String content) {
        if (senderId == null || receiverId == null || senderId.equals(receiverId)) {
            throw new IllegalArgumentException("发送方与接收方不能相同且不能为空");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("消息内容不能为空");
        }
        MessageDO msg = new MessageDO();
        msg.setSenderId(senderId);
        msg.setReceiverId(receiverId);
        msg.setMsgType(type != null ? type : MessageDO.MsgType.TEXT);
        String trimmed = content.trim();
        msg.setContent(trimmed);
        msg.setPayload(trimmed != null ? trimmed : "");
        msg = messageRepository.save(msg);

        String preview = toPreview(type, content);
        LocalDateTime now = msg.getCreateTime();

        updateOrCreateSession(senderId, receiverId, msg.getId(), preview, now, false);
        updateOrCreateSession(receiverId, senderId, msg.getId(), preview, now, true);
        return msg;
    }

    private void updateOrCreateSession(Long userId, Long peerId, Long lastMessageId, String preview, LocalDateTime lastMessageAt, boolean incrementUnread) {
        Optional<MessageSessionDO> opt = messageSessionRepository.findByUserIdAndPeerId(userId, peerId);
        MessageSessionDO session;
        if (opt.isPresent()) {
            session = opt.get();
        } else {
            session = new MessageSessionDO();
            session.setUserId(userId);
            session.setPeerId(peerId);
            session.setCreateTime(lastMessageAt);
        }
        session.setLastMessageId(lastMessageId);
        session.setLastMessagePreview(preview);
        session.setLastMessageAt(lastMessageAt);
        session.setUpdateTime(LocalDateTime.now());
        if (incrementUnread) {
            session.setUnreadCount((session.getUnreadCount() != null ? session.getUnreadCount() : 0) + 1);
        }
        messageSessionRepository.save(session);
    }

    private static String toPreview(MessageDO.MsgType type, String content) {
        if (content == null) return "";
        if (type == MessageDO.MsgType.FORWARD_CARD) {
            return "[转发卡片]";
        }
        String s = content.trim();
        if (s.length() <= PREVIEW_MAX_LEN) return s;
        return s.substring(0, PREVIEW_MAX_LEN) + "...";
    }

    @Transactional(readOnly = true)
    public Page<MessageDO> getChatHistory(Long userId, Long peerId, Pageable pageable) {
        return messageRepository.findChatBetween(userId, peerId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<MessageSessionDO> getRecentSessions(Long userId, Pageable pageable) {
        return messageSessionRepository.findByUserIdOrderByLastMessageAtDesc(userId, pageable);
    }
}
