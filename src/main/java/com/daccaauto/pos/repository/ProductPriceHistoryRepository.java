package com.daccaauto.pos.repository;

import com.daccaauto.pos.entity.ProductPriceHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductPriceHistoryRepository extends JpaRepository<ProductPriceHistoryEntity, Long> {

    boolean existsByProductId(Long productId);

    List<ProductPriceHistoryEntity> findTop8ByOrderByCreatedAtDesc();

    List<ProductPriceHistoryEntity> findTop10ByStoreIdAndProductIdOrderByCreatedAtDesc(
        Long storeId,
        Long productId
    );

    ProductPriceHistoryEntity findFirstByStoreIdAndProductIdOrderByCreatedAtDesc(
        Long storeId,
        Long productId
    );
}
