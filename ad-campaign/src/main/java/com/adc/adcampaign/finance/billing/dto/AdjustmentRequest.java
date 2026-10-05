package com.adc.adcampaign.finance.billing.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdjustmentRequest {
    /** Optional — omit for a tenant-level adjustment not tied to one campaign. */
    private Long campaignId;

    @NotNull
    private BigDecimal amount;
}
