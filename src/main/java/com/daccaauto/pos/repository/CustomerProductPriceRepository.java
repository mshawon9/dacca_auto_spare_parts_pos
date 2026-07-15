package com.daccaauto.pos.repository;

import com.daccaauto.pos.entity.CustomerProductPriceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerProductPriceRepository extends JpaRepository<CustomerProductPriceEntity, Long> {

    Optional<CustomerProductPriceEntity> findByCustomerIdAndProductId(Long customerId, Long productId);
}
