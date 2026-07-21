package com.daccaauto.pos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "customers",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_customer_name", columnNames = "name")
        }
)
public class CustomerEntity extends BaseEntity {

    @NotBlank
    @Size(max = 150)
    @Column(nullable = false, length = 150)
    private String name;

    @Size(max = 100)
    @Column(name = "contact_person", length = 100)
    private String contactPerson;

    @Size(max = 30)
    @Column(length = 30)
    private String phone;

    @Email
    @Size(max = 120)
    @Column(length = 120)
    private String email;

    @Size(max = 50)
    @Pattern(
            regexp = "^[A-Za-z0-9\\-]*$",
            message = "TRN number must be alphanumeric or hyphen"
    )
    @Column(name = "trn_number", length = 50)
    private String trnNumber;

    @Size(max = 255)
    @Column(length = 255)
    private String address;

    @Column(nullable = false)
    private boolean active = true;

    @ColumnDefault("false")
    @Column(name = "always_credit", nullable = false)
    private boolean alwaysCredit = false;

    @Column(name = "default_credit_days")
    private Integer defaultCreditDays;
}
