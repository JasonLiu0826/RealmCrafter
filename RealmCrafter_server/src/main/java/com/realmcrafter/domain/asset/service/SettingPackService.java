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
     *
     * @param allowDownload 允许克隆下载，默认 true
     * @param allowModify   允许二次修改，默认 true
     */
    @Transactional
    public SettingPackDO create(Long userId, String title, String cover, String description,
                                SettingContentDTO content, String deviceHash,
                                Boolean allowDownload, Boolean allowModify) {
        String id = AssetIdGenerator.generateId("sd", userId);

        SettingPackDO pack = new SettingPackDO();
        pack.setId(id);
        pack.setUserId(userId);
        pack.setTitle(title);
        pack.setCover(cover);
        pack.setDescription(description);
        pack.setContent(content);
        pack.setDeviceHash(deviceHash);
        pack.setAllowDownload(allowDownload != null ? allowDownload : true);
        pack.setAllowModify(allowModify != null ? allowModify : true);

        return settingPackRepository.save(pack);
    }

    /**
     * 获取公开设定集（用于 Fork 等场景，不校验归属）。
     */
    @Transactional(readOnly = true)
    public SettingPackDO getPublicById(String id) {
        SettingPackDO pack = settingPackRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("设定集不存在"));
        if (pack.getStatus() != SettingPackDO.AssetStatus.NORMAL) {
            throw new IllegalArgumentException("设定集不可用");
        }
        if (!Boolean.TRUE.equals(pack.getIsPublic())) {
            throw new IllegalArgumentException("该设定集未公开，无法 Fork");
        }
        return pack;
    }

    /**
     * Fork 设定集：深拷贝并创建新 ID，sourceSettingId 永远保留以实现血统溯源。
     * <p>
     * 权限矩阵：
     * - allowDownload=false：禁止 Fork，抛业务异常
     * - allowDownload=true + allowModify=false：可 Fork，但副本锁定，禁止更新
     * - allowDownload=true + allowModify=true：可 Fork 且可编辑
     */
    @Transactional
    public SettingPackDO forkSetting(String sourceId, Long userId) {
        SettingPackDO source = getPublicById(sourceId);
        if (source.getUserId().equals(userId)) {
            throw new IllegalArgumentException("不能 Fork 自己的设定集");
        }
        if (!Boolean.TRUE.equals(source.getAllowDownload())) {
            throw new IllegalArgumentException("该设定集仅支持云端引用，禁止克隆下载");
        }

        String newId = AssetIdGenerator.generateId("sd", userId);
        SettingPackDO fork = new SettingPackDO();
        fork.setId(newId);
        fork.setUserId(userId);
        fork.setSourceSettingId(sourceId);
        fork.setTitle(source.getTitle());
        fork.setCover(source.getCover());
        fork.setDescription(source.getDescription());
        fork.setContent(source.getContent());
        fork.setAllowDownload(source.getAllowDownload());
        fork.setAllowModify(source.getAllowModify());
        fork.setIsPublic(false);

        SettingPackDO saved = settingPackRepository.save(fork);

        source.setForkCount((source.getForkCount() != null ? source.getForkCount() : 0) + 1);
        settingPackRepository.save(source);

        return saved;
    }

    /**
     * 更新设定集内容，依赖 JPA @Version 进行乐观锁校验。
     * 若为 Fork 副本且 allowModify=false，禁止更新（五大维度锁定）。
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
                                String deviceHash,
                                Boolean allowDownload,
                                Boolean allowModify) {
        SettingPackDO pack = getByIdAndUser(id, userId);
        if (pack.getSourceSettingId() != null && !Boolean.TRUE.equals(pack.getAllowModify())) {
            throw new IllegalArgumentException("该设定集为锁定副本，禁止修改");
        }
        pack.setTitle(title);
        pack.setCover(cover);
        pack.setDescription(description);
        pack.setContent(content);
        pack.setDeviceHash(deviceHash);
        pack.setVersionId(expectedVersion);
        if (pack.getSourceSettingId() == null) {
            if (allowDownload != null) pack.setAllowDownload(allowDownload);
            if (allowModify != null) pack.setAllowModify(allowModify);
        }

        try {
            return settingPackRepository.save(pack);
        } catch (OptimisticLockingFailureException e) {
            throw new SyncConflictException("设定集存在更新冲突，请先同步最新内容后再保存。");
        }
    }
}

