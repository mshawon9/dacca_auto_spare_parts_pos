package com.daccaauto.pos.service;

import com.daccaauto.pos.dto.sale.*;

import java.util.List;

public interface SaleService {

    List<SaleDraftSummary> openDrafts();

    SaleDraftResponse createDraft();

    SaleDraftResponse getDraft(Long id);

    SaleDraftResponse updateHeader(Long id, SaleDraftHeaderRequest request);

    SaleDraftResponse addLine(Long id, SaleDraftLineRequest request);

    SaleDraftResponse removeLine(Long id, Long lineId);

    void deleteDraft(Long id);

    SaleResponse complete(Long id);

    List<SelectOption> searchCustomers(String keyword);

    List<SaleProductOption> searchProducts(String keyword, Long storeId, Long customerId);

    SaleProductOption productInfo(Long productId, Long storeId, Long customerId);
}
