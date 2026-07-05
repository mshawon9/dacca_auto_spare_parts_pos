package com.daccaauto.pos.service;

import com.daccaauto.pos.dto.product.ProductImportResult;
import org.springframework.web.multipart.MultipartFile;

public interface ProductImportService {

    ProductImportResult importProducts(MultipartFile file);

    byte[] buildSampleTemplate();
}
