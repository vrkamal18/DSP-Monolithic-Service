package com.adc.adcampaign.campaign.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BudgetRequest {
    @NotNull
    private Long campaignId;

    @NotNull
    private BigDecimal totalBudget;

    private BigDecimal dailyCap;
    private BigDecimal spentAmount;
    private String pacingMode;
}
