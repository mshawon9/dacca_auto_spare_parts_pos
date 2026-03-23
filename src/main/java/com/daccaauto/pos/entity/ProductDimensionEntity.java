package com.daccaauto.pos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Embeddable
public class ProductDimensionEntity {

    @DecimalMin(value = "0.0", inclusive = false)
    @Digits(integer = 10, fraction = 2)
    @Column(name = "length_mm", precision = 12, scale = 2)
    private BigDecimal lengthMm;

    @DecimalMin(value = "0.0", inclusive = false)
    @Digits(integer = 10, fraction = 2)
    @Column(name = "width_mm", precision = 12, scale = 2)
    private BigDecimal widthMm;

    @DecimalMin(value = "0.0", inclusive = false)
    @Digits(integer = 10, fraction = 2)
    @Column(name = "height_mm", precision = 12, scale = 2)
    private BigDecimal heightMm;

    @DecimalMin(value = "0.0", inclusive = false)
    @Digits(integer = 10, fraction = 2)
    @Column(name = "weight_gram", precision = 12, scale = 2)
    private BigDecimal weightGram;
}