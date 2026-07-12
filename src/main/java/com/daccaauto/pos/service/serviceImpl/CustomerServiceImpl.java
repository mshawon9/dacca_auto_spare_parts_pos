package com.daccaauto.pos.service.serviceImpl;

import com.daccaauto.pos.dto.customer.CustomerCreateRequest;
import com.daccaauto.pos.dto.customer.CustomerResponse;
import com.daccaauto.pos.entity.CustomerEntity;
import com.daccaauto.pos.exception.DuplicateResourceException;
import com.daccaauto.pos.exception.ResourceNotFoundException;
import com.daccaauto.pos.repository.CustomerRepository;
import com.daccaauto.pos.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;

    @Override
    public CustomerResponse create(CustomerCreateRequest request) {
        String name = request.getName().trim();
        if (customerRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("Customer already exists: " + name);
        }

        CustomerEntity entity = new CustomerEntity();
        entity.setName(name);
        entity.setContactPerson(trimToNull(request.getContactPerson()));
        entity.setPhone(trimToNull(request.getPhone()));
        entity.setEmail(trimToNull(request.getEmail()));
        entity.setAddress(trimToNull(request.getAddress()));
        entity.setTrnNumber(trimToNull(request.getTrnNumber()));
        entity.setActive(request.getActive() == null || request.getActive());
        return map(customerRepository.save(entity));
    }

    @Override
    public CustomerResponse update(Long id, CustomerCreateRequest request) {
        CustomerEntity entity = customerRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id));
        String name = request.getName().trim();
        if (customerRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new DuplicateResourceException("Customer already exists: " + name);
        }

        entity.setName(name);
        entity.setContactPerson(trimToNull(request.getContactPerson()));
        entity.setPhone(trimToNull(request.getPhone()));
        entity.setEmail(trimToNull(request.getEmail()));
        entity.setAddress(trimToNull(request.getAddress()));
        entity.setTrnNumber(trimToNull(request.getTrnNumber()));
        entity.setActive(request.getActive() == null || request.getActive());
        return map(customerRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerResponse> getPage(Pageable pageable) {
        return customerRepository.findAllByOrderByNameAsc(pageable).map(this::map);
    }

    @Override
    public void delete(Long id) {
        CustomerEntity entity = customerRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id));
        customerRepository.delete(entity);
    }

    private CustomerResponse map(CustomerEntity entity) {
        return new CustomerResponse(
            entity.getId(),
            entity.getName(),
            entity.getContactPerson(),
            entity.getPhone(),
            entity.getEmail(),
            entity.getAddress(),
            entity.getTrnNumber(),
            entity.isActive()
        );
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
