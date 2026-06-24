package com.daccaauto.pos.repository;

import com.daccaauto.pos.entity.ProductSimilarityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductSimilarityRepository extends JpaRepository<ProductSimilarityEntity, Long> {

    @Query("""
        select ps
        from ProductSimilarityEntity ps
        join fetch ps.productTwo
        where ps.productOne.id = :productId
        """)
    Optional<ProductSimilarityEntity> findSelectedSimilarity(Long productId);

    @Modifying
    @Query("delete from ProductSimilarityEntity ps where ps.productOne.id = :productId")
    void deleteByProductOneId(Long productId);

    @Query(value = """
        with recursive similarity_group(product_id) as (
            select cast(:productId as bigint)
            union
            select case
                when ps.product_one_id = sg.product_id then ps.product_two_id
                else ps.product_one_id
            end
            from product_similarities ps
            join similarity_group sg
              on ps.product_one_id = sg.product_id
              or ps.product_two_id = sg.product_id
        )
        select product_id
        from similarity_group
        where product_id <> :productId
        """, nativeQuery = true)
    List<Long> findSimilarityGroupProductIds(Long productId);

    @Modifying
    @Query("""
        delete from ProductSimilarityEntity ps
        where ps.productOne.id = :productId or ps.productTwo.id = :productId
        """)
    void deleteAllForProduct(Long productId);
}
