package com.daccaauto.pos.service;

import com.daccaauto.pos.dto.purchase.PurchaseOrderRequest;
import com.daccaauto.pos.dto.purchase.PurchaseOrderResponse;
import com.daccaauto.pos.dto.purchase.PurchaseOrderDetail;
import com.daccaauto.pos.dto.purchase.PurchaseOrderListItem;
import com.daccaauto.pos.dto.purchase.PurchaseReturnRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PurchaseOrderService {

    PurchaseOrderResponse create(PurchaseOrderRequest request);

    PurchaseOrderResponse update(Long id, PurchaseOrderRequest request);

    PurchaseOrderDetail getDetail(Long id);

    PurchaseOrderRequest getForm(Long id);

    Page<PurchaseOrderListItem> search(String keyword, Pageable pageable);

    void returnItem(Long orderId, PurchaseReturnRequest request);
}
