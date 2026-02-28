package com.realmcrafter.infrastructure.persistence.repository;

import com.realmcrafter.infrastructure.persistence.entity.AssetLikeDO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AssetLikeRepository extends JpaRepository<AssetLikeDO, Long> {

    Optional<AssetLikeDO> findByUserIdAndAssetTypeAndAssetId(Long userId, AssetLikeDO.AssetType assetType, String assetId);

    boolean existsByUserIdAndAssetTypeAndAssetId(Long userId, AssetLikeDO.AssetType assetType, String assetId);
}
