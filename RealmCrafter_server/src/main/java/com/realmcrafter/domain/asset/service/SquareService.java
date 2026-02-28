package com.realmcrafter.domain.asset.service;

import com.realmcrafter.domain.user.ExpAction;
import com.realmcrafter.domain.user.service.UserExpService;
import com.realmcrafter.infrastructure.persistence.entity.SettingPackDO;
import com.realmcrafter.infrastructure.persistence.entity.StoryDO;
import com.realmcrafter.infrastructure.persistence.repository.SettingPackRepository;
import com.realmcrafter.infrastructure.persistence.repository.StoryRepository;
import com.realmcrafter.infrastructure.persistence.repository.UserRepository;
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
 * TRAFFIC 排序依赖定时任务写库的 traffic_weight 字段。
 */
@Service
@RequiredArgsConstructor
public class SquareService {

    private final StoryRepository storyRepository;
    private final SettingPackRepository settingPackRepository;
    private final UserRepository userRepository;
    private final UserExpService userExpService;

    public enum SquareSort {
        NEWEST,
        HOT,
        /** 按流量权重（含等级系数）排序 */
        TRAFFIC
    }

    @Transactional(readOnly = true)
    public Page<StoryDO> listPublicStories(SquareSort sort, String keyword, Pageable pageable, Long userId) {
        if (userId != null) {
            userExpService.addExp(userId, ExpAction.FETCH_FROM_SQUARE);
        }
        if (sort == SquareSort.TRAFFIC) {
            if (keyword != null && !keyword.isBlank()) {
                return storyRepository.findPublicStoriesByKeywordOrderByTrafficWeightDesc(StoryDO.Status.NORMAL, keyword.trim(), pageable);
            }
            return storyRepository.findByStatusAndIsPublicOrderByTrafficWeightDescCreateTimeDesc(StoryDO.Status.NORMAL, true, pageable);
        }
        Pageable withSort = toPageable(sort, pageable, true);
        if (keyword != null && !keyword.isBlank()) {
            return storyRepository.findPublicStoriesByKeyword(StoryDO.Status.NORMAL, keyword.trim(), withSort);
        }
        return storyRepository.findByStatusAndIsPublic(StoryDO.Status.NORMAL, true, withSort);
    }

    @Transactional(readOnly = true)
    public Page<SettingPackDO> listPublicSettings(SquareSort sort, String keyword, Pageable pageable, Long userId) {
        if (userId != null) {
            userExpService.addExp(userId, ExpAction.FETCH_FROM_SQUARE);
        }
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
