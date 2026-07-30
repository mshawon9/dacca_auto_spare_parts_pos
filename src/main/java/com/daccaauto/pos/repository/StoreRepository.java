package com.daccaauto.pos.repository;

import com.daccaauto.pos.entity.StoreEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface StoreRepository extends JpaRepository<StoreEntity, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    long countByActiveTrue();

    Optional<StoreEntity> findByNameIgnoreCase(String name);

    Page<StoreEntity> findAllByOrderByNameAsc(Pageable pageable);

    List<StoreEntity> findAllByActiveTrueOrderByNameAsc();

    @Query("""
        select store
        from StoreEntity store
        where :keywordPattern is null
           or lower(store.name) like :keywordPattern
           or lower(coalesce(store.code, '')) like :keywordPattern
           or lower(coalesce(store.address, '')) like :keywordPattern
        order by store.name asc
        """)
    Page<StoreEntity> searchStores(String keywordPattern, Pageable pageable);
}
