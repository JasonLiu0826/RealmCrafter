package com.realmcrafter.api.user;

import com.realmcrafter.api.dto.Result;
import com.realmcrafter.api.user.dto.EngineConfigDTO;
import com.realmcrafter.api.user.dto.UpdateEngineConfigRequest;
import com.realmcrafter.infrastructure.persistence.entity.UserDO;
import com.realmcrafter.infrastructure.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/users/me/engine-config")
@RequiredArgsConstructor
@Validated
public class UserConfigController {

    private final UserRepository userRepository;

    @GetMapping
    public Result<EngineConfigDTO> getEngineConfig(@RequestHeader("X-User-Id") Long userId) {
        UserDO user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        Double chaosLevel = user.getChaosLevel() != null ? user.getChaosLevel() : 0.7;
        EngineConfigDTO dto = new EngineConfigDTO(chaosLevel, user.getPreferredModel());
        return Result.ok(dto);
    }

    @PatchMapping
    public Result<EngineConfigDTO> updateEngineConfig(@RequestHeader("X-User-Id") Long userId,
                                                      @RequestBody @Valid UpdateEngineConfigRequest request) {
        UserDO user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        if (request.getChaosLevel() != null) {
            double value = request.getChaosLevel();
            if (value < 0.1 || value > 1.0) {
                throw new IllegalArgumentException("混沌阈值必须在 0.1 与 1.0 之间");
            }
            user.setChaosLevel(value);
        }

        if (request.getPreferredModel() != null) {
            user.setPreferredModel(request.getPreferredModel());
        }

        UserDO saved = userRepository.save(user);
        Double chaosLevel = saved.getChaosLevel() != null ? saved.getChaosLevel() : 0.7;
        EngineConfigDTO dto = new EngineConfigDTO(chaosLevel, saved.getPreferredModel());
        return Result.ok(dto);
    }
}

