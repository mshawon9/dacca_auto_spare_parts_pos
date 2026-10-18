package com.daccaauto.pos.service.serviceImpl;

import com.daccaauto.pos.dto.brand.BrandCreateRequest;
import com.daccaauto.pos.dto.brand.BrandResponse;
import com.daccaauto.pos.dto.brand.BrandUpdateRequest;
import com.daccaauto.pos.entity.BrandEntity;
import com.daccaauto.pos.exception.DuplicateResourceException;
import com.daccaauto.pos.exception.ResourceNotFoundException;
import com.daccaauto.pos.repository.BrandRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BrandServiceImplTest {

    private BrandRepository brandRepository;
    private BrandServiceImpl service;

    @BeforeEach
    void setUp() {
        brandRepository = mock(BrandRepository.class);
        service = new BrandServiceImpl(brandRepository);
    }

    @Test
    void createTrimsFieldsAndDefaultsActiveToTrue() {
        when(brandRepository.existsByNameIgnoreCase("Acme")).thenReturn(false);
        when(brandRepository.save(any(BrandEntity.class))).thenAnswer(invocation -> {
            BrandEntity entity = invocation.getArgument(0);
            entity.setId(12L);
            return entity;
        });
        BrandCreateRequest request = new BrandCreateRequest();
        request.setName("  Acme  ");
        request.setCode("  ");
        request.setActive(null);

        BrandResponse response = service.create(request);

        assertEquals(12L, response.id());
        assertEquals("Acme", response.name());
        assertNull(response.code());
        assertTrue(response.active());
    }

    @Test
    void createRejectsDuplicateNameWithoutSaving() {
        when(brandRepository.existsByNameIgnoreCase("Acme")).thenReturn(true);
        BrandCreateRequest request = new BrandCreateRequest();
        request.setName(" Acme ");

        assertThrows(DuplicateResourceException.class, () -> service.create(request));

        verify(brandRepository, never()).save(any(BrandEntity.class));
    }

    @Test
    void updateTrimsValuesAndPreservesInactiveState() {
        BrandEntity existing = new BrandEntity();
        existing.setId(7L);
        when(brandRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(brandRepository.existsByNameIgnoreCaseAndIdNot("New name", 7L)).thenReturn(false);
        when(brandRepository.save(any(BrandEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        BrandUpdateRequest request = new BrandUpdateRequest();
        request.setName(" New name ");
        request.setCode(" X1 ");
        request.setActive(false);

        BrandResponse response = service.update(7L, request);

        assertEquals("New name", response.name());
        assertEquals("X1", response.code());
        assertFalse(response.active());
    }

    @Test
    void getByIdThrowsWhenBrandDoesNotExist() {
        when(brandRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getById(9L));
    }
}