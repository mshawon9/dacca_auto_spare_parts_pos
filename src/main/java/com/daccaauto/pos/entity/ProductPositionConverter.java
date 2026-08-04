package com.daccaauto.pos.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ProductPositionConverter implements AttributeConverter<ProductPosition, String> {

    @Override
    public String convertToDatabaseColumn(ProductPosition attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public ProductPosition convertToEntityAttribute(String dbData) {
        return ProductPosition.from(dbData).orElse(null);
    }
}
