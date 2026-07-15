package com.daccaauto.pos.repository;

import com.daccaauto.pos.entity.ProductStockEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import java.math.BigDecimal;

import java.util.List;
import java.util.Optional;

public interface ProductStockRepository extends JpaRepository<ProductStockEntity, Long> {

    boolean existsByProductId(Long productId);

    @Query("select count(ps) from ProductStockEntity ps where ps.quantity = 0")
    long countZeroQuantity();

    long countBySellingPriceIsNull();

    @Query("select coalesce(sum(ps.quantity), 0) from ProductStockEntity ps")
    BigDecimal sumQuantity();

    @Query("select coalesce(sum(ps.quantity), 0) from ProductStockEntity ps where ps.product.id = :productId")
    BigDecimal sumQuantityByProductId(Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select ps
        from ProductStockEntity ps
        where ps.store.id = :storeId and ps.product.id = :productId
        """)
    Optional<ProductStockEntity> findForUpdate(Long storeId, Long productId);

    Optional<ProductStockEntity> findByStoreIdAndProductId(Long storeId, Long productId);

    List<ProductStockEntity> findByStoreIdAndProductIdIn(Long storeId, List<Long> productIds);

    List<ProductStockEntity> findByProductIdOrderByStoreNameAsc(Long productId);
}
