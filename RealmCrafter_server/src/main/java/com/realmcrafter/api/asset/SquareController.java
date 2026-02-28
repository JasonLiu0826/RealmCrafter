package com.realmcrafter.api.asset;

import com.realmcrafter.api.dto.Result;
import com.realmcrafter.domain.asset.service.SquareService;
import com.realmcrafter.infrastructure.persistence.entity.SettingPackDO;
import com.realmcrafter.infrastructure.persistence.entity.StoryDO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 广场发现：故事广场与设定广场，仅展示 is_public = true 且 status = NORMAL 的资产。
 */
@RestController
@RequestMapping("/api/v1/square")
@RequiredArgsConstructor
@Validated
public class SquareController {

    private final SquareService squareService;

    @GetMapping("/stories")
    public Result<Page<StoryDO>> listStories(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(defaultValue = "NEWEST") String sort,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        SquareService.SquareSort sortEnum = "TRAFFIC".equalsIgnoreCase(sort) ? SquareService.SquareSort.TRAFFIC : "HOT".equalsIgnoreCase(sort) ? SquareService.SquareSort.HOT : SquareService.SquareSort.NEWEST;
        Pageable pageable = PageRequest.of(page, size);
        return Result.ok(squareService.listPublicStories(sortEnum, keyword, pageable, userId));
    }

    @GetMapping("/settings")
    public Result<Page<SettingPackDO>> listSettings(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(defaultValue = "NEWEST") String sort,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        SquareService.SquareSort sortEnum = "HOT".equalsIgnoreCase(sort) ? SquareService.SquareSort.HOT : SquareService.SquareSort.NEWEST;
        Pageable pageable = PageRequest.of(page, size);
        return Result.ok(squareService.listPublicSettings(sortEnum, keyword, pageable, userId));
    }
}
