package com.daccaauto.pos.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "product_similarities",
    indexes = {
        @Index(name = "idx_product_similarity_one", columnList = "product_one_id"),
        @Index(name = "idx_product_similarity_two", columnList = "product_two_id")
    }
)
public class ProductSimilarityEntity extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_one_id", nullable = false)
    private ProductEntity productOne;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_two_id", nullable = false)
    private ProductEntity productTwo;
}
