package com.realmcrafter.api.asset;

import com.realmcrafter.api.asset.dto.CreateSettingRequest;
import com.realmcrafter.api.asset.dto.UpdateSettingRequest;
import com.realmcrafter.api.dto.Result;
import com.realmcrafter.domain.asset.service.SettingPackService;
import com.realmcrafter.infrastructure.persistence.entity.SettingPackDO;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
@Validated
public class SettingPackController {

    private final SettingPackService settingPackService;

    @GetMapping
    public Result<List<SettingPackDO>> list(@RequestHeader("X-User-Id") Long userId) {
        return Result.ok(settingPackService.listByUser(userId));
    }

    @PostMapping
    public Result<SettingPackDO> create(@RequestHeader("X-User-Id") Long userId,
                                        @RequestBody @Valid CreateSettingRequest request) {
        SettingPackDO created = settingPackService.create(
                userId,
                request.getTitle(),
                request.getCover(),
                request.getDescription(),
                request.getContent(),
                request.getDeviceHash()
        );
        return Result.ok(created);
    }

    @GetMapping("/{id}")
    public Result<SettingPackDO> detail(@PathVariable("id") String id,
                                        @RequestHeader("X-User-Id") Long userId) {
        return Result.ok(settingPackService.getByIdAndUser(id, userId));
    }

    @PutMapping("/{id}")
    public Result<SettingPackDO> update(@PathVariable("id") String id,
                                        @RequestHeader("X-User-Id") Long userId,
                                        @RequestBody @Valid UpdateSettingRequest request) {
        SettingPackDO updated = settingPackService.update(
                id,
                userId,
                request.getVersionId(),
                request.getTitle(),
                request.getCover(),
                request.getDescription(),
                request.getContent(),
                request.getDeviceHash()
        );
        return Result.ok(updated);
    }
}

