package com.daccaauto.pos.service.serviceImpl;

import com.daccaauto.pos.dto.store.StoreManageRequest;
import com.daccaauto.pos.dto.store.StoreManageResponse;
import com.daccaauto.pos.entity.StoreEntity;
import com.daccaauto.pos.exception.DuplicateResourceException;
import com.daccaauto.pos.exception.ResourceNotFoundException;
import com.daccaauto.pos.repository.StoreRepository;
import com.daccaauto.pos.service.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class StoreServiceImpl implements StoreService {

    private final StoreRepository storeRepository;

    @Override
    public StoreManageResponse create(StoreManageRequest request) {
        StoreEntity entity = new StoreEntity();
        applyRequest(entity, request, null);
        return map(storeRepository.save(entity));
    }

    @Override
    public StoreManageResponse update(Long id, StoreManageRequest request) {
        StoreEntity entity = storeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Store not found: " + id));

        applyRequest(entity, request, id);
        return map(storeRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StoreManageResponse> getPage(String keyword, Pageable pageable) {
        String keywordPattern = keyword == null || keyword.isBlank()
            ? null
            : "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";

        return storeRepository.searchStores(keywordPattern, pageable).map(this::map);
    }

    @Override
    public void delete(Long id) {
        StoreEntity entity = storeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Store not found: " + id));

        try {
            storeRepository.delete(entity);
            storeRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateResourceException(
                "Cannot delete this store because inventory, sales, purchase orders, or stock history are using it. Deactivate it instead."
            );
        }
    }

    private void applyRequest(StoreEntity entity, StoreManageRequest request, Long currentId) {
        String name = request.getName().trim();
        boolean duplicateName = currentId == null
            ? storeRepository.existsByNameIgnoreCase(name)
            : storeRepository.existsByNameIgnoreCaseAndIdNot(name, currentId);

        if (duplicateName) {
            throw new DuplicateResourceException("Store or shop already exists: " + name);
        }

        entity.setName(name);
        entity.setCode(trimToNull(request.getCode()));
        entity.setAddress(trimToNull(request.getAddress()));
        entity.setActive(request.getActive() == null || request.getActive());
    }

    private StoreManageResponse map(StoreEntity entity) {
        return new StoreManageResponse(
            entity.getId(),
            entity.getName(),
            entity.getCode(),
            entity.getAddress(),
            entity.isActive(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
