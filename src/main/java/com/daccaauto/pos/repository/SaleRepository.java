package com.daccaauto.pos.repository;

import com.daccaauto.pos.entity.SaleEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
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

    @EntityGraph(attributePaths = {"customer", "store"})
    @Query(
        value = """
            select s from SaleEntity s
            left join s.customer c
            join s.store st
            where s.balanceDue > 0
              and (:hasKeyword = false
                or lower(s.invoiceNo) like :keyword
                or lower(coalesce(c.name, '')) like :keyword
                or lower(st.name) like :keyword)
              and (:customerId = 0 or c.id = :customerId)
              and (:monthlyOnly = false or s.saleType = com.daccaauto.pos.entity.SaleType.MONTHLY_STATEMENT)
              and (:regularOnly = false or s.saleType <> com.daccaauto.pos.entity.SaleType.MONTHLY_STATEMENT)
              and (:overdueOnly = false or (s.dueDate is not null and s.dueDate < :today))
            """,
        countQuery = """
            select count(s) from SaleEntity s
            left join s.customer c
            join s.store st
            where s.balanceDue > 0
              and (:hasKeyword = false
                or lower(s.invoiceNo) like :keyword
                or lower(coalesce(c.name, '')) like :keyword
                or lower(st.name) like :keyword)
              and (:customerId = 0 or c.id = :customerId)
              and (:monthlyOnly = false or s.saleType = com.daccaauto.pos.entity.SaleType.MONTHLY_STATEMENT)
              and (:regularOnly = false or s.saleType <> com.daccaauto.pos.entity.SaleType.MONTHLY_STATEMENT)
              and (:overdueOnly = false or (s.dueDate is not null and s.dueDate < :today))
            """
    )
    Page<SaleEntity> searchOpenCredits(@Param("keyword") String keyword,
                                       @Param("hasKeyword") boolean hasKeyword,
                                       @Param("customerId") Long customerId,
                                       @Param("monthlyOnly") boolean monthlyOnly,
                                       @Param("regularOnly") boolean regularOnly,
                                       @Param("overdueOnly") boolean overdueOnly,
                                       @Param("today") LocalDate today,
                                       Pageable pageable);

    @Query("""
        select count(s),
               coalesce(sum(s.balanceDue), 0),
               coalesce(sum(case when s.dueDate is not null and s.dueDate < :today then s.balanceDue else 0 end), 0)
        from SaleEntity s
        left join s.customer c
        join s.store st
        where s.balanceDue > 0
          and (:hasKeyword = false
            or lower(s.invoiceNo) like :keyword
            or lower(coalesce(c.name, '')) like :keyword
            or lower(st.name) like :keyword)
          and (:customerId = 0 or c.id = :customerId)
          and (:monthlyOnly = false or s.saleType = com.daccaauto.pos.entity.SaleType.MONTHLY_STATEMENT)
          and (:regularOnly = false or s.saleType <> com.daccaauto.pos.entity.SaleType.MONTHLY_STATEMENT)
          and (:overdueOnly = false or (s.dueDate is not null and s.dueDate < :today))
        """)
    Object[] summarizeOpenCredits(@Param("keyword") String keyword,
                                  @Param("hasKeyword") boolean hasKeyword,
                                  @Param("customerId") Long customerId,
                                  @Param("monthlyOnly") boolean monthlyOnly,
                                  @Param("regularOnly") boolean regularOnly,
                                  @Param("overdueOnly") boolean overdueOnly,
                                  @Param("today") LocalDate today);

    @Query("""
        select count(s),
               coalesce(sum(s.total), 0),
               coalesce(sum(s.paidAmount), 0),
               coalesce(sum(s.balanceDue), 0)
        from SaleEntity s
        join s.customer c
        where c.id = :customerId
          and s.saleType = com.daccaauto.pos.entity.SaleType.MONTHLY_STATEMENT
          and s.saleDate >= :fromDate
          and s.saleDate <= :toDate
          and s.balanceDue > 0
        """)
    Object[] summarizeMonthlyStatementCredit(@Param("customerId") Long customerId,
                                             @Param("fromDate") LocalDate fromDate,
                                             @Param("toDate") LocalDate toDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select s from SaleEntity s
        join fetch s.customer c
        join fetch s.store st
        where c.id = :customerId
          and s.saleType = com.daccaauto.pos.entity.SaleType.MONTHLY_STATEMENT
          and s.saleDate >= :fromDate
          and s.saleDate <= :toDate
          and s.balanceDue > 0
        order by s.saleDate asc, s.id asc
        """)
    List<SaleEntity> findMonthlyStatementCreditsForUpdate(@Param("customerId") Long customerId,
                                                          @Param("fromDate") LocalDate fromDate,
                                                          @Param("toDate") LocalDate toDate);

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
