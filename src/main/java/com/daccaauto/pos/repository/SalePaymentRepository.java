package com.daccaauto.pos.repository;

import com.daccaauto.pos.entity.SalePaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface SalePaymentRepository extends JpaRepository<SalePaymentEntity, Long> {

    List<SalePaymentEntity> findBySaleIdOrderByReceiveDateDescIdDesc(Long saleId);

    @Query("""
        select p from SalePaymentEntity p
        join fetch p.sale s
        join fetch s.customer c
        join fetch s.store st
        where c.id = :customerId
          and p.receiveDate >= :fromDate
          and p.receiveDate <= :toDate
        order by p.receiveDate asc, p.id asc
        """)
    List<SalePaymentEntity> findCustomerPayments(@Param("customerId") Long customerId,
                                                 @Param("fromDate") LocalDate fromDate,
                                                 @Param("toDate") LocalDate toDate);
}
