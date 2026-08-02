package com.daccaauto.pos.repository;

import com.daccaauto.pos.entity.ProductApplicationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductApplicationRepository extends JpaRepository<ProductApplicationEntity, Long> {

    boolean existsByProductIdAndVehicleApplicationId(Long productId, Long vehicleApplicationId);

    void deleteByProductId(Long productId);

    @Query("""
        select pa
        from ProductApplicationEntity pa
        join fetch pa.vehicleApplication va
        where pa.product.id = :productId
        order by va.displayName asc
        """)
    List<ProductApplicationEntity> findByProductIdWithApplication(Long productId);
}
