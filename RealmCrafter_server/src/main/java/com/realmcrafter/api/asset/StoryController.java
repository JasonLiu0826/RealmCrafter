package com.realmcrafter.api.asset;

import com.realmcrafter.api.asset.dto.CreateStoryRequest;
import com.realmcrafter.api.asset.dto.RenameStoryRequest;
import com.realmcrafter.api.dto.Result;
import com.realmcrafter.domain.asset.service.StoryService;
import com.realmcrafter.infrastructure.persistence.entity.StoryDO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/stories")
@RequiredArgsConstructor
@Validated
public class StoryController {

    private final StoryService storyService;

    @GetMapping
    public Result<Page<StoryDO>> list(@RequestHeader("X-User-Id") Long userId,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size,
                                      @RequestParam(required = false) String keyword) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updateTime"));
        return Result.ok(storyService.listByUser(userId, keyword, pageable));
    }

    @PostMapping
    public Result<StoryDO> create(@RequestBody @Valid CreateStoryRequest request) {
        StoryDO created = storyService.create(
                request.getUserId(),
                request.getSettingPackId(),
                request.getTitle(),
                request.getCover(),
                request.getDescription()
        );
        return Result.ok(created);
    }

    @PatchMapping("/{id}/rename")
    public Result<StoryDO> rename(@PathVariable("id") String id,
                                  @RequestBody @Valid RenameStoryRequest request) {
        StoryDO updated = storyService.rename(id, request.getUserId(), request.getTitle());
        return Result.ok(updated);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") String id,
                               @RequestHeader("X-User-Id") Long userId) {
        storyService.delete(id, userId);
        return Result.ok();
    }
}

