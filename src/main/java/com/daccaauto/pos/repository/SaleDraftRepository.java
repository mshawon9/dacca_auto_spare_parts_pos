package com.daccaauto.pos.repository;

import com.daccaauto.pos.entity.SaleDraftEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SaleDraftRepository extends JpaRepository<SaleDraftEntity, Long> {

    @EntityGraph(attributePaths = {"customer", "store", "lines", "lines.product", "lines.product.brand", "lines.product.category"})
    Optional<SaleDraftEntity> findWithLinesById(Long id);

    List<SaleDraftEntity> findTop10ByOrderByUpdatedAtDesc();
}
