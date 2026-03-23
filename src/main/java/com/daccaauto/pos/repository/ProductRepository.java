package com.daccaauto.pos.repository;

import com.daccaauto.pos.entity.ProductEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

    boolean existsByBrandIdAndNormalizedPartNumber(Long brandId, String normalizedPartNumber);

    @Query("""
            select distinct p
            from ProductEntity p
            left join p.brand b
            left join p.category c
            left join p.applications pa
            left join pa.vehicleApplication va
            left join va.vehicleMake mk
            left join va.vehicleModel vm
            where p.active = true
              and (
                    lower(p.name) like lower(concat('%', :q, '%'))
                 or lower(coalesce(p.specLabel, '')) like lower(concat('%', :q, '%'))
                 or lower(p.partNumber) like lower(concat('%', :q, '%'))
                 or lower(coalesce(p.barcode, '')) like lower(concat('%', :q, '%'))
                 or lower(b.name) like lower(concat('%', :q, '%'))
                 or lower(c.name) like lower(concat('%', :q, '%'))
                 or lower(va.displayName) like lower(concat('%', :q, '%'))
                 or lower(mk.name) like lower(concat('%', :q, '%'))
                 or lower(vm.name) like lower(concat('%', :q, '%'))
              )
            order by b.name asc, p.partNumber asc
            """)
    List<ProductEntity> searchForDropdown(@Param("q") String q, Pageable pageable);
}