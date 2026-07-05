package com.daccaauto.pos.dto.product;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
public class ProductCreateRequest {

    @NotBlank
    @Size(max = 200)
    private String name;

    @Size(max = 120)
    private String specLabel;

    @Size(max = 80)
    private String position;

    @Size(max = 120)
    private String dimension;

    @Size(max = 100)
    private String sku;

    @NotNull
    @DecimalMin("0.000")
    @Digits(integer = 16, fraction = 3)
    private BigDecimal reorderLevel = BigDecimal.valueOf(2);

    @NotBlank
    @Size(max = 100)
    @Pattern(regexp = "^[A-Za-z0-9._/\\-]+$", message = "Part number cannot contain spaces")
    private String partNumber;

    @Size(max = 255)
    @Pattern(regexp = "^[A-Za-z0-9._/\\- ,]*$")
    private String alternativePartNumber;

    @Size(max = 64)
    @Pattern(regexp = "^[A-Za-z0-9\\-]*$")
    private String barcode;

    @Size(max = 2000)
    private String description;

    @NotNull
    private Long categoryId;

    @NotNull
    private Long brandId;

    private Set<Long> applicationIds = new LinkedHashSet<>();

    private Long similarProductId;

    private MultipartFile image;

    private Boolean active = true;
}
