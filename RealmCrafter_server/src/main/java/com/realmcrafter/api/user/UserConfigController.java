package com.realmcrafter.api.user;

import com.realmcrafter.api.dto.Result;
import com.realmcrafter.api.user.dto.EngineConfigDTO;
import com.realmcrafter.api.user.dto.UpdateEngineConfigRequest;
import com.realmcrafter.infrastructure.persistence.entity.UserConfigDO;
import com.realmcrafter.infrastructure.persistence.repository.UserConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Optional;

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
    @Transactional
    public Result<EngineConfigDTO> updateEngineConfig(@RequestHeader("X-User-Id") Long userId,
                                                      @RequestBody @Valid UpdateEngineConfigRequest request) {
        Double chaosLevel = request.getChaosLevel() != null ? request.getChaosLevel() : 0.7;
        if (chaosLevel < 0.1 || chaosLevel > 1.0) {
            throw new IllegalArgumentException("混沌阈值必须在 0.1 与 1.0 之间");
        }
        String preferredModel = request.getPreferredModel() != null ? request.getPreferredModel() : "realm_crafter_v1";
        int memoryDepth = 4000;

        Optional<UserConfigDO> optionalConfig = userConfigRepository.findById(userId);
        if (optionalConfig.isPresent()) {
            UserConfigDO config = optionalConfig.get();
            config.setChaosLevel(chaosLevel);
            config.setPreferredModel(preferredModel);
            userConfigRepository.save(config);
        } else {
            userConfigRepository.upsertEngineConfig(userId, preferredModel, chaosLevel, memoryDepth);
        }
        return Result.ok(new EngineConfigDTO(chaosLevel, preferredModel));
    }

    private UserConfigDO defaultConfig(Long userId) {
        UserConfigDO c = new UserConfigDO();
        c.setUserId(userId);
        return c;
    }
}

