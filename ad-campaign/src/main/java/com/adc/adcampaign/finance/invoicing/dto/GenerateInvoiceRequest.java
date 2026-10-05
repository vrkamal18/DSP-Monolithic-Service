package com.adc.adcampaign.finance.invoicing.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class GenerateInvoiceRequest {
    @NotNull
    private Long advertiserId;

    @NotNull
    private LocalDate periodStart;

    @NotNull
    private LocalDate periodEnd;
}
