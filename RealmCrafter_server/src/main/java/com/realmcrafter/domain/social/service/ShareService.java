package com.realmcrafter.domain.social.service;

import com.realmcrafter.domain.user.ExpAction;
import com.realmcrafter.domain.user.service.UserExpService;
import com.realmcrafter.infrastructure.persistence.entity.ShareRecordDO;
import com.realmcrafter.infrastructure.persistence.repository.ShareRecordRepository;
import com.realmcrafter.infrastructure.persistence.repository.StoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Map;
import java.util.Optional;

/**
 * TikTok 式转发：生成短链与深度链接解析，并产出站内 IM 转发卡片 payload。
 */
@Service
@RequiredArgsConstructor
public class ShareService {

    private static final String SHORT_CODE_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int SHORT_CODE_LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Value("${realmcrafter.share.base-url:https://realmcrafter.app}")
    private String shareBaseUrl;

    private final ShareRecordRepository shareRecordRepository;
    private final StoryRepository storyRepository;
    private final UserExpService userExpService;

    /**
     * 生成分享记录，返回短链与站内转发卡片 payload（供消息模块发送 FORWARD_CARD）。
     */
    @Transactional
    public ShareResult generate(ShareRecordDO.ShareType type, String storyId, Long chapterId,
                                 String targetRef, String excerpt) {
        String shortCode = generateUniqueShortCode();
        ShareRecordDO record = new ShareRecordDO();
        record.setShortCode(shortCode);
        record.setType(type);
        record.setStoryId(storyId);
        record.setChapterId(chapterId);
        record.setTargetRef(targetRef != null ? targetRef : "");
        record.setExcerpt(excerpt);
        shareRecordRepository.save(record);

        storyRepository.findById(storyId).ifPresent(story -> userExpService.addExp(story.getUserId(), ExpAction.BE_SHARED));

        String deepLink = shareBaseUrl + "/s/" + shortCode;
        Map<String, Object> forwardCardPayload = Map.of(
                "type", type.name(),
                "storyId", storyId,
                "chapterId", chapterId,
                "targetRef", targetRef != null ? targetRef : "",
                "excerpt", excerpt != null ? excerpt : ""
        );
        return new ShareResult(shortCode, deepLink, forwardCardPayload);
    }

    /**
     * 解析短链：外部用户点击深度链接后，前置调用此接口拿到锚点信息并定位到段落/选项/评论。
     */
    public Optional<ShareTarget> decode(String shortCode) {
        return shareRecordRepository.findById(shortCode)
                .map(r -> new ShareTarget(r.getType(), r.getStoryId(), r.getChapterId(), r.getTargetRef(), r.getExcerpt()));
    }

    private String generateUniqueShortCode() {
        for (int i = 0; i < 10; i++) {
            String code = randomShortCode();
            if (!shareRecordRepository.findById(code).isPresent()) {
                return code;
            }
        }
        throw new IllegalStateException("Failed to generate unique short code");
    }

    private static String randomShortCode() {
        StringBuilder sb = new StringBuilder(SHORT_CODE_LENGTH);
        for (int i = 0; i < SHORT_CODE_LENGTH; i++) {
            sb.append(SHORT_CODE_CHARS.charAt(RANDOM.nextInt(SHORT_CODE_CHARS.length())));
        }
        return sb.toString();
    }

    public static final class ShareResult {
        public final String shortCode;
        public final String deepLink;
        public final Map<String, Object> forwardCardPayload;

        public ShareResult(String shortCode, String deepLink, Map<String, Object> forwardCardPayload) {
            this.shortCode = shortCode;
            this.deepLink = deepLink;
            this.forwardCardPayload = forwardCardPayload;
        }
    }

    public static final class ShareTarget {
        public final ShareRecordDO.ShareType type;
        public final String storyId;
        public final Long chapterId;
        public final String targetRef;
        public final String excerpt;

        public ShareTarget(ShareRecordDO.ShareType type, String storyId, Long chapterId, String targetRef, String excerpt) {
            this.type = type;
            this.storyId = storyId;
            this.chapterId = chapterId;
            this.targetRef = targetRef != null ? targetRef : "";
            this.excerpt = excerpt;
        }
    }
}
