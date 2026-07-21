package com.daccaauto.pos.dto.sale;

import com.daccaauto.pos.entity.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CreditCollectionRequest {

    @NotNull
    private LocalDate receiveDate = LocalDate.now();

    @NotNull
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    @NotNull
    private PaymentMethod paymentMethod = PaymentMethod.CASH;

    private LocalDate chequeDate;

    @Size(max = 80)
    private String chequeNumber;

    private LocalDate dueDate;

    @Size(max = 250)
    private String note;
}
