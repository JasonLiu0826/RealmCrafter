package com.realmcrafter.application.admin;

import com.realmcrafter.infrastructure.persistence.entity.StoryDO;
import com.realmcrafter.infrastructure.persistence.entity.UserDO;
import com.realmcrafter.infrastructure.persistence.repository.StoryRepository;
import com.realmcrafter.infrastructure.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 平台管理：封禁用户、下架故事、授予金牌创作者。
 */
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final StoryRepository storyRepository;

    /** 校验当前用户是否为管理员 */
    public boolean isAdmin(Long userId) {
        if (userId == null) return false;
        return userRepository.findById(userId)
                .map(u -> u.getRole() == UserDO.UserRole.ADMIN || u.getRole() == UserDO.UserRole.SUPER_ADMIN)
                .orElse(false);
    }

    /** 封禁用户：sealedUntil 之后自动解封，null 表示永久（或需人工解封）。 */
    @Transactional
    public Optional<String> sealUser(Long targetUserId, LocalDateTime sealedUntil) {
        UserDO user = userRepository.findById(targetUserId).orElse(null);
        if (user == null) return Optional.of("用户不存在");
        user.setSealedUntil(sealedUntil);
        userRepository.save(user);
        return Optional.empty();
    }

    /** 强制下架故事（违规等） */
    @Transactional
    public Optional<String> takeDownStory(String storyId) {
        StoryDO story = storyRepository.findById(storyId).orElse(null);
        if (story == null) return Optional.of("故事不存在");
        story.setStatus(StoryDO.Status.TAKEN_DOWN);
        storyRepository.save(story);
        return Optional.empty();
    }

    /** 授予金牌创作者标识 */
    @Transactional
    public Optional<String> grantGoldenCreator(Long targetUserId) {
        UserDO user = userRepository.findById(targetUserId).orElse(null);
        if (user == null) return Optional.of("用户不存在");
        user.setIsGoldenCreator(true);
        userRepository.save(user);
        return Optional.empty();
    }
}
