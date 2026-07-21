package com.daccaauto.pos.repository;

import com.daccaauto.pos.entity.SalePaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SalePaymentRepository extends JpaRepository<SalePaymentEntity, Long> {

    List<SalePaymentEntity> findBySaleIdOrderByReceiveDateDescIdDesc(Long saleId);
}
