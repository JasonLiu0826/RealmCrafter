package com.realmcrafter.application.schedule;

import com.realmcrafter.domain.asset.service.TrafficWeightComputeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时任务：将广场故事流量权重写库，供 TRAFFIC 排序使用，应对日活过万、上万本书场景。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TrafficWeightJob {

    private final TrafficWeightComputeService trafficWeightComputeService;

    /** 每 5 分钟重算一次流量权重 */
    @Scheduled(cron = "${realmcrafter.job.traffic-weight.cron:0 */5 * * * ?}")
    public void run() {
        try {
            trafficWeightComputeService.computeAndWriteAll();
        } catch (Exception e) {
            log.warn("TrafficWeightJob failed", e);
        }
    }
}
