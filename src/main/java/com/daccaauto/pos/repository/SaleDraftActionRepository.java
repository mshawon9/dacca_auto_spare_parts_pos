package com.daccaauto.pos.repository;

import com.daccaauto.pos.entity.SaleDraftActionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaleDraftActionRepository extends JpaRepository<SaleDraftActionEntity, Long> {

    void deleteByDraftId(Long draftId);
}
