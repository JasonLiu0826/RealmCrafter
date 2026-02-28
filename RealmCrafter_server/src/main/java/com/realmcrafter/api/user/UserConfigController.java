package com.realmcrafter.api.user;

import com.realmcrafter.api.dto.Result;
import com.realmcrafter.api.user.dto.EngineConfigDTO;
import com.realmcrafter.api.user.dto.UpdateEngineConfigRequest;
import com.realmcrafter.infrastructure.persistence.entity.UserConfigDO;
import com.realmcrafter.infrastructure.persistence.repository.UserConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/users/me/engine-config")
@RequiredArgsConstructor
@Validated
public class UserConfigController {

    private final UserConfigRepository userConfigRepository;

    @GetMapping
    public Result<EngineConfigDTO> getEngineConfig(@RequestHeader("X-User-Id") Long userId) {
        UserConfigDO config = userConfigRepository.findById(userId)
                .orElseGet(() -> defaultConfig(userId));

        Double chaosLevel = config.getChaosLevel() != null ? config.getChaosLevel() : 0.7;
        String preferredModel = config.getPreferredModel() != null ? config.getPreferredModel() : "realm_crafter_v1";
        return Result.ok(new EngineConfigDTO(chaosLevel, preferredModel));
    }

    @PatchMapping
    public Result<EngineConfigDTO> updateEngineConfig(@RequestHeader("X-User-Id") Long userId,
                                                      @RequestBody @Valid UpdateEngineConfigRequest request) {
        UserConfigDO config = userConfigRepository.findById(userId)
                .orElseGet(() -> defaultConfig(userId));

        if (request.getChaosLevel() != null) {
            double value = request.getChaosLevel();
            if (value < 0.1 || value > 1.0) {
                throw new IllegalArgumentException("混沌阈值必须在 0.1 与 1.0 之间");
            }
            config.setChaosLevel(value);
        }

        if (request.getPreferredModel() != null) {
            config.setPreferredModel(request.getPreferredModel());
        }

        UserConfigDO saved = userConfigRepository.save(config);
        Double chaosLevel = saved.getChaosLevel() != null ? saved.getChaosLevel() : 0.7;
        String preferredModel = saved.getPreferredModel() != null ? saved.getPreferredModel() : "realm_crafter_v1";
        return Result.ok(new EngineConfigDTO(chaosLevel, preferredModel));
    }

    private UserConfigDO defaultConfig(Long userId) {
        UserConfigDO c = new UserConfigDO();
        c.setUserId(userId);
        return c;
    }
}

