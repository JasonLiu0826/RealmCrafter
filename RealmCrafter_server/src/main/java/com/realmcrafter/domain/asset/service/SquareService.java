package com.realmcrafter.domain.asset.service;

import com.realmcrafter.infrastructure.persistence.entity.SettingPackDO;
import com.realmcrafter.infrastructure.persistence.entity.StoryDO;
import com.realmcrafter.infrastructure.persistence.repository.SettingPackRepository;
import com.realmcrafter.infrastructure.persistence.repository.StoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 广场发现引擎：仅展示 is_public = true 且 status = NORMAL 的资产，支持排序与关键词搜索。
 */
@Service
@RequiredArgsConstructor
public class SquareService {

    private final StoryRepository storyRepository;
    private final SettingPackRepository settingPackRepository;

    public enum SquareSort {
        NEWEST,
        HOT
    }

    @Transactional(readOnly = true)
    public Page<StoryDO> listPublicStories(SquareSort sort, String keyword, Pageable pageable) {
        Pageable withSort = toPageable(sort, pageable, true);
        if (keyword != null && !keyword.isBlank()) {
            return storyRepository.findPublicStoriesByKeyword(StoryDO.Status.NORMAL, keyword.trim(), withSort);
        }
        return storyRepository.findByStatusAndIsPublic(StoryDO.Status.NORMAL, true, withSort);
    }

    @Transactional(readOnly = true)
    public Page<SettingPackDO> listPublicSettings(SquareSort sort, String keyword, Pageable pageable) {
        Pageable withSort = toPageable(sort, pageable, false);
        if (keyword != null && !keyword.isBlank()) {
            return settingPackRepository.findPublicSettingsByKeyword(SettingPackDO.AssetStatus.NORMAL, keyword.trim(), withSort);
        }
        return settingPackRepository.findByStatusAndIsPublic(SettingPackDO.AssetStatus.NORMAL, true, withSort);
    }

    private Pageable toPageable(SquareSort sort, Pageable pageable, boolean isStory) {
        List<Sort.Order> orders = new ArrayList<>();
        if (sort == SquareSort.HOT) {
            orders.add(Sort.Order.desc("likesCount"));
            orders.add(Sort.Order.desc("forkCount"));
        }
        orders.add(Sort.Order.desc("createTime"));
        return org.springframework.data.domain.PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(orders)
        );
    }
}
