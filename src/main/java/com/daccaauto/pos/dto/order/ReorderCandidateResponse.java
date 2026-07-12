package com.daccaauto.pos.dto.order;

import java.math.BigDecimal;

public record ReorderCandidateResponse(
    Long productId,
    String productName,
    String partNumber,
    BigDecimal currentQuantity,
    BigDecimal reorderLevel,
    boolean alreadyInTodo
) {
}
