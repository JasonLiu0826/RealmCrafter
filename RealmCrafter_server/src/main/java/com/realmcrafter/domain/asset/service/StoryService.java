package com.realmcrafter.domain.asset.service;

import com.realmcrafter.infrastructure.id.AssetIdGenerator;
import com.realmcrafter.infrastructure.persistence.entity.ChapterDO;
import com.realmcrafter.infrastructure.persistence.entity.SettingPackDO;
import com.realmcrafter.infrastructure.persistence.entity.StoryDO;
import com.realmcrafter.infrastructure.persistence.entity.UserDO;
import com.realmcrafter.infrastructure.persistence.entity.WalletTransactionDO;
import com.realmcrafter.infrastructure.persistence.repository.ChapterRepository;
import com.realmcrafter.infrastructure.persistence.repository.SettingPackRepository;
import com.realmcrafter.infrastructure.persistence.repository.StoryRepository;
import com.realmcrafter.infrastructure.persistence.repository.UserRepository;
import com.realmcrafter.infrastructure.persistence.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 故事（书架）领域服务。
 */
@Service
@RequiredArgsConstructor
public class StoryService {

    private static final BigDecimal CREATOR_SHARE = new BigDecimal("0.70");

    private final StoryRepository storyRepository;
    private final SettingPackRepository settingPackRepository;
    private final ChapterRepository chapterRepository;
    private final UserRepository userRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    @Transactional(readOnly = true)
    public StoryDO getById(String id, Long userId) {
        StoryDO story = storyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("故事不存在"));
        if (story.getStatus() != StoryDO.Status.NORMAL) {
            throw new IllegalArgumentException("故事不存在或已下架");
        }
        if (userId != null && story.getUserId().equals(userId)) {
            return story;
        }
        if (!Boolean.TRUE.equals(story.getIsPublic())) {
            throw new IllegalArgumentException("无权访问该故事");
        }
        return story;
    }

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

    /**
     * 硬核 Fork：复制故事 + 深拷贝所有章节，血统记录 sourceStoryId；若原作为付费则扣费并 70% 分润给原作者。
     */
    @Transactional
    public StoryDO forkStory(String sourceStoryId, Long forkUserId) {
        StoryDO original = storyRepository.findById(sourceStoryId)
                .orElseThrow(() -> new IllegalArgumentException("故事不存在"));
        if (original.getStatus() != StoryDO.Status.NORMAL) {
            throw new IllegalArgumentException("该故事不可 Fork");
        }
        if (!Boolean.TRUE.equals(original.getIsPublic())) {
            throw new IllegalArgumentException("该故事未公开，无法 Fork");
        }
        if (original.getUserId().equals(forkUserId)) {
            throw new IllegalArgumentException("不能 Fork 自己的故事");
        }

        BigDecimal price = original.getPrice() != null ? original.getPrice() : BigDecimal.ZERO;
        if (price.compareTo(BigDecimal.ZERO) > 0) {
            UserDO forkUser = userRepository.findById(forkUserId)
                    .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
            BigDecimal balance = forkUser.getCrystalBalance() != null ? forkUser.getCrystalBalance() : BigDecimal.ZERO;
            if (balance.compareTo(price) < 0) {
                throw new IllegalArgumentException("灵能水晶不足，无法购买该故事的 Fork 权");
            }
            forkUser.setCrystalBalance(balance.subtract(price));
            userRepository.save(forkUser);

            BigDecimal creatorAmount = price.multiply(CREATOR_SHARE).setScale(2, RoundingMode.HALF_UP);
            UserDO author = userRepository.findById(original.getUserId()).orElse(null);
            if (author != null) {
                BigDecimal authorBalance = author.getCrystalBalance() != null ? author.getCrystalBalance() : BigDecimal.ZERO;
                author.setCrystalBalance(authorBalance.add(creatorAmount));
                userRepository.save(author);
                WalletTransactionDO tx = new WalletTransactionDO();
                tx.setUserId(original.getUserId());
                tx.setAmount(creatorAmount);
                tx.setType(WalletTransactionDO.Type.CREATOR_REVENUE);
                tx.setDescription("Fork 分润：故事 " + sourceStoryId + " 被 Fork");
                walletTransactionRepository.save(tx);
            }
        }

        String newId = AssetIdGenerator.generateId("gs", forkUserId);
        StoryDO newStory = new StoryDO();
        newStory.setId(newId);
        newStory.setUserId(forkUserId);
        newStory.setSettingPackId(original.getSettingPackId());
        newStory.setSourceStoryId(original.getId());
        newStory.setTitle(original.getTitle());
        newStory.setCover(original.getCover());
        newStory.setDescription(original.getDescription());
        newStory.setPrice(BigDecimal.ZERO);
        newStory.setAllowDownload(original.getAllowDownload());
        newStory.setCloneIncludesSettings(original.getCloneIncludesSettings());
        newStory.setIsPublic(false);
        newStory.setStatus(StoryDO.Status.NORMAL);
        newStory.setLastChapterIndex(original.getLastChapterIndex() != null ? original.getLastChapterIndex() : 0);
        newStory.setLikesCount(0);
        newStory.setForkCount(0);
        storyRepository.save(newStory);

        List<ChapterDO> chapters = chapterRepository.findByStoryIdOrderByChapterIndexAsc(sourceStoryId);
        for (ChapterDO ch : chapters) {
            ChapterDO clone = new ChapterDO();
            clone.setStoryId(newId);
            clone.setChapterIndex(ch.getChapterIndex());
            clone.setTitle(ch.getTitle());
            clone.setContent(ch.getContent());
            clone.setBranchesData(ch.getBranchesData());
            chapterRepository.save(clone);
        }

        original.setForkCount((original.getForkCount() != null ? original.getForkCount() : 0) + 1);
        storyRepository.save(original);

        return newStory;
    }
}

