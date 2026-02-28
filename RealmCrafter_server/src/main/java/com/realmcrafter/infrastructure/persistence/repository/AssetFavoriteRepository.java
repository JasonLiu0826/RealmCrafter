package com.realmcrafter.infrastructure.persistence.repository;

import com.realmcrafter.infrastructure.persistence.entity.AssetFavoriteDO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AssetFavoriteRepository extends JpaRepository<AssetFavoriteDO, Long> {

    Optional<AssetFavoriteDO> findByUserIdAndAssetTypeAndAssetId(Long userId, AssetFavoriteDO.AssetType assetType, String assetId);

    boolean existsByUserIdAndAssetTypeAndAssetId(Long userId, AssetFavoriteDO.AssetType assetType, String assetId);
}
