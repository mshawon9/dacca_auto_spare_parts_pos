package com.daccaauto.pos.service.serviceImpl;

import com.daccaauto.pos.dto.order.ReorderCandidateResponse;
import com.daccaauto.pos.dto.order.OrderTodoResponse;
import com.daccaauto.pos.entity.OrderTodoEntity;
import com.daccaauto.pos.entity.OrderTodoStatus;
import com.daccaauto.pos.entity.ProductEntity;
import com.daccaauto.pos.exception.DuplicateResourceException;
import com.daccaauto.pos.exception.ResourceNotFoundException;
import com.daccaauto.pos.repository.OrderTodoRepository;
import com.daccaauto.pos.repository.ProductRepository;
import com.daccaauto.pos.repository.ProductStockRepository;
import com.daccaauto.pos.service.OrderTodoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderTodoServiceImpl implements OrderTodoService {

    private static final List<OrderTodoStatus> OPEN_STATUSES = List.of(
        OrderTodoStatus.PENDING,
        OrderTodoStatus.ORDERED,
        OrderTodoStatus.RECEIVED
    );

    private final OrderTodoRepository orderTodoRepository;
    private final ProductRepository productRepository;
    private final ProductStockRepository productStockRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<OrderTodoResponse> getOpenTodos(String keyword, Pageable pageable) {
        return orderTodoRepository.searchOpenTodos(
            OPEN_STATUSES,
            normalizeKeyword(keyword),
            pageable
        ).map(this::map);
    }

    @Override
    public void syncReorderTodos() {
        productRepository.findProductsAtOrBelowReorderLevel(PageRequest.of(0, 500))
            .forEach(item -> orderTodoRepository
                .findFirstByProductIdAndStatusIn(item.getProductId(), OPEN_STATUSES)
                .ifPresentOrElse(existing -> {
                    existing.setCurrentQuantity(item.getTotalQuantity());
                    existing.setReorderLevel(item.getReorderLevel());
                    orderTodoRepository.save(existing);
                }, () -> createTodo(item.getProductId(), item.getTotalQuantity(), item.getReorderLevel())));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReorderCandidateResponse> getReorderCandidates() {
        return productRepository.findProductsAtOrBelowReorderLevel(PageRequest.of(0, 500))
            .stream()
            .map(item -> new ReorderCandidateResponse(
                item.getProductId(),
                item.getProductName(),
                item.getPartNumber(),
                item.getTotalQuantity(),
                item.getReorderLevel(),
                orderTodoRepository.findFirstByProductIdAndStatusIn(item.getProductId(), OPEN_STATUSES).isPresent()
            ))
            .toList();
    }

    @Override
    public void addSelectedReorderTodos(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return;
        }

        for (Long productId : productIds) {
            createManualTodo(productId, null);
        }
    }

    @Override
    public void createManualTodo(Long productId, String note) {
        ProductEntity product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
        if (!product.isActive()) {
            throw new DuplicateResourceException("Cannot add inactive product to order todo");
        }

        BigDecimal totalQuantity = productStockRepository.sumQuantityByProductId(productId);
        BigDecimal reorderLevel = product.getReorderLevel() == null ? BigDecimal.valueOf(2) : product.getReorderLevel();

        orderTodoRepository.findFirstByProductIdAndStatusIn(productId, OPEN_STATUSES)
            .ifPresentOrElse(existing -> {
                existing.setCurrentQuantity(totalQuantity);
                existing.setReorderLevel(reorderLevel);
                existing.setStatus(OrderTodoStatus.PENDING);
                existing.setNote(trimToNull(note));
                orderTodoRepository.save(existing);
            }, () -> {
                OrderTodoEntity todo = new OrderTodoEntity();
                todo.setProduct(product);
                todo.setCurrentQuantity(totalQuantity);
                todo.setReorderLevel(reorderLevel);
                todo.setStatus(OrderTodoStatus.PENDING);
                todo.setNote(trimToNull(note));
                orderTodoRepository.save(todo);
            });
    }

    @Override
    public void updateStatus(Long id, OrderTodoStatus status, String note) {
        OrderTodoEntity todo = orderTodoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Order todo not found: " + id));
        todo.setStatus(status);
        todo.setNote(trimToNull(note));
        orderTodoRepository.save(todo);
    }

    @Override
    public void markReceived(Long id) {
        OrderTodoEntity todo = orderTodoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Order todo not found: " + id));
        todo.setStatus(OrderTodoStatus.RECEIVED);
        orderTodoRepository.save(todo);
    }

    @Override
    public void cleanupForProduct(Long productId) {
        ProductEntity product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            return;
        }

        BigDecimal totalQuantity = productStockRepository.sumQuantityByProductId(productId);
        BigDecimal reorderLevel = product.getReorderLevel() == null ? BigDecimal.valueOf(2) : product.getReorderLevel();
        orderTodoRepository.findByProductIdAndStatusIn(productId, OPEN_STATUSES)
            .forEach(todo -> {
                todo.setCurrentQuantity(totalQuantity);
                todo.setReorderLevel(reorderLevel);
                orderTodoRepository.save(todo);
            });
    }

    private void createTodo(Long productId, BigDecimal totalQuantity, BigDecimal reorderLevel) {
        ProductEntity product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

        OrderTodoEntity todo = new OrderTodoEntity();
        todo.setProduct(product);
        todo.setCurrentQuantity(totalQuantity);
        todo.setReorderLevel(reorderLevel);
        todo.setStatus(OrderTodoStatus.PENDING);
        orderTodoRepository.save(todo);
    }

    private OrderTodoResponse map(OrderTodoEntity todo) {
        ProductEntity product = todo.getProduct();
        return new OrderTodoResponse(
            todo.getId(),
            product.getId(),
            product.getName(),
            product.getPartNumber(),
            product.getCategory().getName(),
            product.getBrand().getName(),
            todo.getCurrentQuantity(),
            todo.getReorderLevel(),
            todo.getStatus(),
            todo.getNote(),
            todo.getUpdatedAt()
        );
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return "%" + keyword.trim().toLowerCase(java.util.Locale.ROOT) + "%";
    }
}
