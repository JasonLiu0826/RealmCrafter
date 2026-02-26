package com.realmcrafter.infrastructure.persistence.repository;

import com.realmcrafter.infrastructure.persistence.entity.UserDO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserDO, Long> {

    Optional<UserDO> findByUsername(String username);

    boolean existsByUsername(String username);
}
