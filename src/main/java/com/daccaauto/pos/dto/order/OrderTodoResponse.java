package com.daccaauto.pos.dto.order;

import com.daccaauto.pos.entity.OrderTodoStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record OrderTodoResponse(
    Long id,
    Long productId,
    String productName,
    String partNumber,
    String categoryName,
    String brandName,
    BigDecimal currentQuantity,
    BigDecimal reorderLevel,
    OrderTodoStatus status,
    String note,
    OffsetDateTime updatedAt
) {
}
