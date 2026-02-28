package com.realmcrafter.domain.asset.service;

import com.realmcrafter.domain.user.LevelCoefficientResolver;
import com.realmcrafter.infrastructure.persistence.entity.StoryDO;
import com.realmcrafter.infrastructure.persistence.entity.UserDO;
import com.realmcrafter.infrastructure.persistence.repository.StoryRepository;
import com.realmcrafter.infrastructure.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 流量权重计算并写库：供定时任务调用，应对日活过万、上万本书时广场 TRAFFIC 排序不依赖内存。
 * 公式：trafficWeight = (likesCount + forkCount*2) * 创作者等级系数。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrafficWeightComputeService {

    private static final int BATCH_SIZE = 200;

    private final StoryRepository storyRepository;
    private final UserRepository userRepository;

    /**
     * 全量重算所有公开故事的 traffic_weight 并写库，按批处理避免长事务与 OOM。
     */
    public void computeAndWriteAll() {
        Pageable page = PageRequest.of(0, BATCH_SIZE, Sort.by("id"));
        int total = 0;
        while (true) {
            List<StoryDO> batch = storyRepository.findByStatusAndIsPublic(
                    StoryDO.Status.NORMAL, true, page).getContent();
            if (batch.isEmpty()) break;
            writeBatch(batch);
            total += batch.size();
            if (batch.size() < BATCH_SIZE) break;
            page = PageRequest.of(page.getPageNumber() + 1, BATCH_SIZE, Sort.by("id"));
        }
        log.debug("TrafficWeight computeAndWriteAll done, stories updated={}", total);
    }

    @Transactional
    public void writeBatch(List<StoryDO> stories) {
        if (stories.isEmpty()) return;
        List<Long> authorIds = stories.stream().map(StoryDO::getUserId).distinct().collect(Collectors.toList());
        Map<Long, UserDO> userMap = userRepository.findAllById(authorIds).stream().collect(Collectors.toMap(UserDO::getId, u -> u));

        for (StoryDO s : stories) {
            int level = 1;
            boolean golden = false;
            UserDO u = userMap.get(s.getUserId());
            if (u != null) {
                level = u.getLevel() != null ? u.getLevel() : 1;
                golden = Boolean.TRUE.equals(u.getIsGoldenCreator());
            }
            long likes = s.getLikesCount() != null ? s.getLikesCount() : 0;
            long forks = s.getForkCount() != null ? s.getForkCount() : 0;
            double base = likes + forks * 2.0;
            double weight = base * LevelCoefficientResolver.trafficWeightCoefficient(level, golden);
            s.setTrafficWeight(weight);
        }
        storyRepository.saveAll(stories);
    }
}
