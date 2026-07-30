package com.daccaauto.pos.service;

import com.daccaauto.pos.dto.store.StoreManageRequest;
import com.daccaauto.pos.dto.store.StoreManageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StoreService {

    StoreManageResponse create(StoreManageRequest request);

    StoreManageResponse update(Long id, StoreManageRequest request);

    Page<StoreManageResponse> getPage(String keyword, Pageable pageable);

    void delete(Long id);
}
