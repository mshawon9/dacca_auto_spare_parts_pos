package com.daccaauto.pos.repository;

import com.daccaauto.pos.entity.SaleEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface SaleRepository extends JpaRepository<SaleEntity, Long> {

    @EntityGraph(attributePaths = {"customer", "store"})
    @Query(
        value = """
            select distinct s from SaleEntity s
            left join s.customer c
            join s.store st
            left join s.lines l
            left join l.product p
            left join p.brand b
            where (:hasKeyword = false
                or lower(s.invoiceNo) like :keyword
                or lower(coalesce(c.name, '')) like :keyword
                or lower(st.name) like :keyword
                or lower(p.name) like :keyword
                or lower(p.partNumber) like :keyword
                or lower(coalesce(p.alternativePartNumber, '')) like :keyword
                or lower(b.name) like :keyword)
              and s.saleDate >= :fromDate
              and s.saleDate <= :toDate
              and (:customerId = 0 or c.id = :customerId)
              and (:creditOnly = false or s.balanceDue > 0)
            """,
        countQuery = """
            select count(distinct s) from SaleEntity s
            left join s.customer c
            join s.store st
            left join s.lines l
            left join l.product p
            left join p.brand b
            where (:hasKeyword = false
                or lower(s.invoiceNo) like :keyword
                or lower(coalesce(c.name, '')) like :keyword
                or lower(st.name) like :keyword
                or lower(p.name) like :keyword
                or lower(p.partNumber) like :keyword
                or lower(coalesce(p.alternativePartNumber, '')) like :keyword
                or lower(b.name) like :keyword)
              and s.saleDate >= :fromDate
              and s.saleDate <= :toDate
              and (:customerId = 0 or c.id = :customerId)
              and (:creditOnly = false or s.balanceDue > 0)
            """
    )
    Page<SaleEntity> searchSales(@Param("keyword") String keyword,
                                 @Param("hasKeyword") boolean hasKeyword,
                                 @Param("fromDate") LocalDate fromDate,
                                 @Param("toDate") LocalDate toDate,
                                 @Param("customerId") Long customerId,
                                 @Param("creditOnly") boolean creditOnly,
                                 Pageable pageable);

    @Query("""
        select coalesce(sum(s.subTotal), 0),
               coalesce(sum(s.vatAmount), 0),
               coalesce(sum(s.total), 0),
               coalesce(sum(s.paidAmount), 0),
               coalesce(sum(s.balanceDue), 0),
               count(s)
        from SaleEntity s
        left join s.customer c
        where s.saleDate >= :fromDate
          and s.saleDate <= :toDate
          and (:customerId = 0 or c.id = :customerId)
          and (:creditOnly = false or s.balanceDue > 0)
        """)
    Object[] summarizeSales(@Param("fromDate") LocalDate fromDate,
                            @Param("toDate") LocalDate toDate,
                            @Param("customerId") Long customerId,
                            @Param("creditOnly") boolean creditOnly);

    @EntityGraph(attributePaths = {"customer", "store", "lines", "lines.product", "lines.product.brand", "lines.product.category"})
    Optional<SaleEntity> findWithLinesById(Long id);

    @Query("select coalesce(max(s.id), 0) from SaleEntity s")
    Long findMaxIdForInvoice();
}
