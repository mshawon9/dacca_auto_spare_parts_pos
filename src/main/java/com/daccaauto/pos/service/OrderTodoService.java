package com.daccaauto.pos.service;

import com.daccaauto.pos.dto.order.ReorderCandidateResponse;
import com.daccaauto.pos.dto.order.OrderTodoResponse;
import com.daccaauto.pos.entity.OrderTodoStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OrderTodoService {

    Page<OrderTodoResponse> getOpenTodos(String keyword, Pageable pageable);

    void syncReorderTodos();

    List<ReorderCandidateResponse> getReorderCandidates();

    void addSelectedReorderTodos(List<Long> productIds);

    void createManualTodo(Long productId, String note);

    void updateStatus(Long id, OrderTodoStatus status, String note);

    void markReceived(Long id);

    void cleanupForProduct(Long productId);
}
