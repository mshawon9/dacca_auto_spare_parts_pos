package com.daccaauto.pos.repository;

import com.daccaauto.pos.entity.StockAdjustmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StockAdjustmentRepository extends JpaRepository<StockAdjustmentEntity, Long> {

    boolean existsByProductId(Long productId);

    List<StockAdjustmentEntity> findTop8ByOrderByCreatedAtDesc();
}
