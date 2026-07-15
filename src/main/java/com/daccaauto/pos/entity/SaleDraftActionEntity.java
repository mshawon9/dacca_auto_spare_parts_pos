package com.daccaauto.pos.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "sale_draft_actions", indexes = {
    @Index(name = "idx_sale_draft_action_draft", columnList = "draft_id"),
    @Index(name = "idx_sale_draft_action_created_at", columnList = "created_at")
})
public class SaleDraftActionEntity extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "draft_id", nullable = false)
    private SaleDraftEntity draft;

    @NotBlank
    @Size(max = 40)
    @Column(name = "action_type", nullable = false, length = 40)
    private String actionType;

    @Size(max = 500)
    @Column(length = 500)
    private String details;
}
