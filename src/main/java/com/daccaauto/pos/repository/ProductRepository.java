package com.daccaauto.pos.repository;

import com.daccaauto.pos.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

    long countByActiveTrue();

    boolean existsByBrandIdAndNormalizedPartNumber(Long brandId, String normalizedPartNumber);

    boolean existsByBrandIdAndNormalizedPartNumberAndIdNot(Long brandId, String normalizedPartNumber, Long id);

    @Query("""
        select distinct p
        from ProductEntity p
        left join ProductApplicationEntity pa on pa.product.id = p.id
        left join pa.vehicleApplication va
        where (:keywordPattern is null or
               lower(p.name) like :keywordPattern or
               lower(coalesce(p.specLabel, '')) like :keywordPattern or
               lower(coalesce(p.dimension, '')) like :keywordPattern or
               lower(coalesce(p.sku, '')) like :keywordPattern or
               lower(p.partNumber) like :keywordPattern or
               lower(coalesce(p.barcode, '')) like :keywordPattern or
               lower(coalesce(va.displayName, '')) like :keywordPattern)
          and (:categoryId is null or p.category.id = :categoryId)
          and (:brandId is null or p.brand.id = :brandId)
          and (:applicationId is null or va.id = :applicationId)
          and (:active is null or p.active = :active)
        order by p.name asc, p.partNumber asc
        """)
    List<ProductEntity> search(@Param("keywordPattern") String keywordPattern,
                               @Param("categoryId") Long categoryId,
                               @Param("brandId") Long brandId,
                               @Param("applicationId") Long applicationId,
                               @Param("active") Boolean active);

    @Query(
        value = """
            select distinct p
            from ProductEntity p
            left join ProductApplicationEntity pa on pa.product.id = p.id
            left join pa.vehicleApplication va
            where (:keywordPattern is null or
                   lower(p.name) like :keywordPattern or
                   lower(coalesce(p.specLabel, '')) like :keywordPattern or
                   lower(coalesce(p.dimension, '')) like :keywordPattern or
                   lower(coalesce(p.sku, '')) like :keywordPattern or
                   lower(p.partNumber) like :keywordPattern or
                   lower(coalesce(p.barcode, '')) like :keywordPattern or
                   lower(coalesce(va.displayName, '')) like :keywordPattern)
              and (:categoryId is null or p.category.id = :categoryId)
              and p.active = true
            """,
        countQuery = """
            select count(distinct p.id)
            from ProductEntity p
            left join ProductApplicationEntity pa on pa.product.id = p.id
            left join pa.vehicleApplication va
            where (:keywordPattern is null or
                   lower(p.name) like :keywordPattern or
                   lower(coalesce(p.specLabel, '')) like :keywordPattern or
                   lower(coalesce(p.dimension, '')) like :keywordPattern or
                   lower(coalesce(p.sku, '')) like :keywordPattern or
                   lower(p.partNumber) like :keywordPattern or
                   lower(coalesce(p.barcode, '')) like :keywordPattern or
                   lower(coalesce(va.displayName, '')) like :keywordPattern)
              and (:categoryId is null or p.category.id = :categoryId)
              and p.active = true
            """
    )
    Page<ProductEntity> searchInventoryPage(@Param("keywordPattern") String keywordPattern,
                                            @Param("categoryId") Long categoryId,
                                            Pageable pageable);
}
