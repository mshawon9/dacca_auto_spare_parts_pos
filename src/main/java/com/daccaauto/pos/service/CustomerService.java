package com.daccaauto.pos.service;

import com.daccaauto.pos.dto.customer.CustomerCreateRequest;
import com.daccaauto.pos.dto.customer.CustomerResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomerService {

    CustomerResponse create(CustomerCreateRequest request);

    CustomerResponse update(Long id, CustomerCreateRequest request);

    Page<CustomerResponse> getPage(Pageable pageable);

    void delete(Long id);
}
