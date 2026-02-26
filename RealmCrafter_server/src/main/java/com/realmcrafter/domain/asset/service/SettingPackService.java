package com.realmcrafter.domain.asset.service;

import com.realmcrafter.config.SyncConflictException;
import com.realmcrafter.domain.asset.dto.SettingContentDTO;
import com.realmcrafter.infrastructure.id.AssetIdGenerator;
import com.realmcrafter.infrastructure.persistence.entity.SettingPackDO;
import com.realmcrafter.infrastructure.persistence.repository.SettingPackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 设定集领域服务。
 */
@Service
@RequiredArgsConstructor
public class SettingPackService {

    private final SettingPackRepository settingPackRepository;

    /**
     * 分页/列表获取当前用户的设定集。
     */
    @Transactional(readOnly = true)
    public Page<SettingPackDO> listByUser(Long userId, Pageable pageable) {
        return settingPackRepository.findByUserId(userId, pageable);
    }

    /**
     * 根据 ID 获取当前用户的设定集，校验归属。
     */
    @Transactional(readOnly = true)
    public SettingPackDO getByIdAndUser(String id, Long userId) {
        SettingPackDO pack = settingPackRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("设定集不存在"));
        if (!pack.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权访问该设定集");
        }
        return pack;
    }

    /**
     * 创建设定集。
     */
    @Transactional
    public SettingPackDO create(Long userId, String title, String cover, String description,
                                SettingContentDTO content, String deviceHash) {
        String id = AssetIdGenerator.generateId("sd", userId);

        SettingPackDO pack = new SettingPackDO();
        pack.setId(id);
        pack.setUserId(userId);
        pack.setTitle(title);
        pack.setCover(cover);
        pack.setDescription(description);
        pack.setContent(content);
        pack.setDeviceHash(deviceHash);

        return settingPackRepository.save(pack);
    }

    /**
     * 更新设定集内容，依赖 JPA @Version 进行乐观锁校验。
     *
     * @param id             设定集 ID
     * @param userId         当前用户 ID
     * @param expectedVersion 前端传入的 versionId
     */
    @Transactional
    public SettingPackDO update(String id,
                                Long userId,
                                long expectedVersion,
                                String title,
                                String cover,
                                String description,
                                SettingContentDTO content,
                                String deviceHash) {
        SettingPackDO pack = getByIdAndUser(id, userId);
        pack.setTitle(title);
        pack.setCover(cover);
        pack.setDescription(description);
        pack.setContent(content);
        pack.setDeviceHash(deviceHash);
        pack.setVersionId(expectedVersion);

        try {
            return settingPackRepository.save(pack);
        } catch (OptimisticLockingFailureException e) {
            throw new SyncConflictException("设定集存在更新冲突，请先同步最新内容后再保存。");
        }
    }
}

