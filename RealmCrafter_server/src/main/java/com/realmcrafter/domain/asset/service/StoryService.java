package com.realmcrafter.domain.asset.service;

import com.realmcrafter.infrastructure.id.AssetIdGenerator;
import com.realmcrafter.infrastructure.persistence.entity.SettingPackDO;
import com.realmcrafter.infrastructure.persistence.entity.StoryDO;
import com.realmcrafter.infrastructure.persistence.repository.SettingPackRepository;
import com.realmcrafter.infrastructure.persistence.repository.StoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 故事（书架）领域服务。
 */
@Service
@RequiredArgsConstructor
public class StoryService {

    private final StoryRepository storyRepository;
    private final SettingPackRepository settingPackRepository;

    @Transactional(readOnly = true)
    public Page<StoryDO> listByUser(Long userId, String keyword, Pageable pageable) {
        if (keyword == null || keyword.isBlank()) {
            return storyRepository.findByUserIdAndStatus(userId, StoryDO.Status.NORMAL, pageable);
        }
        return storyRepository.findByUserIdAndStatusAndTitleContainingIgnoreCase(
                userId, StoryDO.Status.NORMAL, keyword.trim(), pageable);
    }

    @Transactional
    public StoryDO create(Long userId,
                          String settingPackId,
                          String title,
                          String cover,
                          String description) {
        SettingPackDO settingPack = settingPackRepository.findById(settingPackId)
                .orElseThrow(() -> new IllegalArgumentException("关联设定集不存在"));
        if (!settingPack.getUserId().equals(userId)) {
            throw new IllegalArgumentException("设定集不属于当前用户，无法创建故事");
        }

        String id = AssetIdGenerator.generateId("gs", userId);

        StoryDO story = new StoryDO();
        story.setId(id);
        story.setUserId(userId);
        story.setSettingPackId(settingPackId);
        story.setTitle(title);
        story.setCover(cover);
        story.setDescription(description);
        story.setStatus(StoryDO.Status.NORMAL);
        story.setLastChapterIndex(0);

        return storyRepository.save(story);
    }

    @Transactional
    public StoryDO rename(String storyId, Long userId, String newTitle) {
        StoryDO story = storyRepository.findById(storyId)
                .orElseThrow(() -> new IllegalArgumentException("故事不存在"));
        if (!story.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权编辑该故事");
        }
        story.setTitle(newTitle);
        return storyRepository.save(story);
    }

    @Transactional
    public void delete(String storyId, Long userId) {
        StoryDO story = storyRepository.findById(storyId)
                .orElseThrow(() -> new IllegalArgumentException("故事不存在"));
        if (!story.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权删除该故事");
        }
        story.setStatus(StoryDO.Status.DELETED);
        storyRepository.save(story);
    }

    /**
     * 阅读脉冲：用户点击书籍进入阅读器时更新最后阅读时间。
     */
    @Transactional
    public StoryDO updateReadTime(String storyId, Long userId) {
        StoryDO story = storyRepository.findById(storyId)
                .orElseThrow(() -> new IllegalArgumentException("故事不存在"));
        if (!story.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权操作该故事");
        }
        story.setLastReadTime(LocalDateTime.now());
        return storyRepository.save(story);
    }
}

