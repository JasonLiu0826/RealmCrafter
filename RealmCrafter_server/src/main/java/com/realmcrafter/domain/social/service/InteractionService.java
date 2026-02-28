package com.realmcrafter.domain.social.service;

import com.realmcrafter.infrastructure.persistence.entity.AssetFavoriteDO;
import com.realmcrafter.infrastructure.persistence.entity.AssetLikeDO;
import com.realmcrafter.infrastructure.persistence.entity.CommentDO;
import com.realmcrafter.infrastructure.persistence.entity.SettingPackDO;
import com.realmcrafter.infrastructure.persistence.entity.StoryDO;
import com.realmcrafter.infrastructure.persistence.repository.AssetLikeRepository;
import com.realmcrafter.infrastructure.persistence.repository.AssetFavoriteRepository;
import com.realmcrafter.infrastructure.persistence.repository.CommentRepository;
import com.realmcrafter.infrastructure.persistence.repository.SettingPackRepository;
import com.realmcrafter.infrastructure.persistence.repository.StoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 社交互动：点赞/取消赞、收藏/取消收藏，幂等更新 DO 计数。
 */
@Service
@RequiredArgsConstructor
public class InteractionService {

    private final AssetLikeRepository assetLikeRepository;
    private final AssetFavoriteRepository assetFavoriteRepository;
    private final StoryRepository storyRepository;
    private final SettingPackRepository settingPackRepository;
    private final CommentRepository commentRepository;

    @Transactional
    public boolean toggleLike(Long userId, String assetType, String assetId) {
        AssetLikeDO.AssetType type = parseAssetType(assetType);
        var existing = assetLikeRepository.findByUserIdAndAssetTypeAndAssetId(userId, type, assetId);
        if (existing.isPresent()) {
            assetLikeRepository.delete(existing.get());
            decrementLikes(type, assetId);
            return false;
        }
        AssetLikeDO like = new AssetLikeDO();
        like.setUserId(userId);
        like.setAssetType(type);
        like.setAssetId(assetId);
        assetLikeRepository.save(like);
        incrementLikes(type, assetId);
        return true;
    }

    @Transactional
    public boolean toggleFavorite(Long userId, String assetType, String assetId) {
        AssetFavoriteDO.AssetType type = "SETTING".equalsIgnoreCase(assetType) ? AssetFavoriteDO.AssetType.SETTING : AssetFavoriteDO.AssetType.STORY;
        var existing = assetFavoriteRepository.findByUserIdAndAssetTypeAndAssetId(userId, type, assetId);
        if (existing.isPresent()) {
            assetFavoriteRepository.delete(existing.get());
            decrementFavorite(type, assetId);
            return false;
        }
        AssetFavoriteDO fav = new AssetFavoriteDO();
        fav.setUserId(userId);
        fav.setAssetType(type);
        fav.setAssetId(assetId);
        assetFavoriteRepository.save(fav);
        incrementFavorite(type, assetId);
        return true;
    }

    private static AssetLikeDO.AssetType parseAssetType(String assetType) {
        if (assetType == null) return AssetLikeDO.AssetType.STORY;
        switch (assetType.toUpperCase()) {
            case "SETTING": return AssetLikeDO.AssetType.SETTING;
            case "COMMENT": return AssetLikeDO.AssetType.COMMENT;
            default: return AssetLikeDO.AssetType.STORY;
        }
    }

    private void incrementLikes(AssetLikeDO.AssetType type, String assetId) {
        if (type == AssetLikeDO.AssetType.COMMENT) {
            try {
                Long commentId = Long.parseLong(assetId);
                commentRepository.findById(commentId).ifPresent(c -> {
                    c.setLikesCount((c.getLikesCount() != null ? c.getLikesCount() : 0) + 1);
                    commentRepository.save(c);
                });
            } catch (NumberFormatException ignored) { }
            return;
        }
        if (type == AssetLikeDO.AssetType.STORY) {
            storyRepository.findById(assetId).ifPresent(s -> {
                s.setLikesCount((s.getLikesCount() != null ? s.getLikesCount() : 0) + 1);
                storyRepository.save(s);
            });
        } else {
            settingPackRepository.findById(assetId).ifPresent(s -> {
                s.setLikesCount((s.getLikesCount() != null ? s.getLikesCount() : 0) + 1);
                settingPackRepository.save(s);
            });
        }
    }

    private void decrementLikes(AssetLikeDO.AssetType type, String assetId) {
        if (type == AssetLikeDO.AssetType.COMMENT) {
            try {
                Long commentId = Long.parseLong(assetId);
                commentRepository.findById(commentId).ifPresent(c -> {
                    c.setLikesCount(Math.max(0, (c.getLikesCount() != null ? c.getLikesCount() : 0) - 1));
                    commentRepository.save(c);
                });
            } catch (NumberFormatException ignored) { }
            return;
        }
        if (type == AssetLikeDO.AssetType.STORY) {
            storyRepository.findById(assetId).ifPresent(s -> {
                s.setLikesCount(Math.max(0, (s.getLikesCount() != null ? s.getLikesCount() : 0) - 1));
                storyRepository.save(s);
            });
        } else {
            settingPackRepository.findById(assetId).ifPresent(s -> {
                s.setLikesCount(Math.max(0, (s.getLikesCount() != null ? s.getLikesCount() : 0) - 1));
                settingPackRepository.save(s);
            });
        }
    }

    private void incrementFavorite(AssetFavoriteDO.AssetType type, String assetId) {
        if (type == AssetFavoriteDO.AssetType.STORY) {
            storyRepository.findById(assetId).ifPresent(s -> {
                s.setFavoriteCount((s.getFavoriteCount() != null ? s.getFavoriteCount() : 0) + 1);
                storyRepository.save(s);
            });
        } else {
            settingPackRepository.findById(assetId).ifPresent(s -> {
                s.setFavoriteCount((s.getFavoriteCount() != null ? s.getFavoriteCount() : 0) + 1);
                settingPackRepository.save(s);
            });
        }
    }

    private void decrementFavorite(AssetFavoriteDO.AssetType type, String assetId) {
        if (type == AssetFavoriteDO.AssetType.STORY) {
            storyRepository.findById(assetId).ifPresent(s -> {
                s.setFavoriteCount(Math.max(0, (s.getFavoriteCount() != null ? s.getFavoriteCount() : 0) - 1));
                storyRepository.save(s);
            });
        } else {
            settingPackRepository.findById(assetId).ifPresent(s -> {
                s.setFavoriteCount(Math.max(0, (s.getFavoriteCount() != null ? s.getFavoriteCount() : 0) - 1));
                settingPackRepository.save(s);
            });
        }
    }
}
