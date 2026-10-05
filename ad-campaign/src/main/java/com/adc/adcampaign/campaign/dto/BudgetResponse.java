package com.adc.adcampaign.campaign.dto;

import com.adc.adcampaign.campaign.entity.Budget;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class BudgetResponse {
    private Long id;
    private Long campaignId;
    private BigDecimal totalBudget;
    private BigDecimal dailyCap;
    private BigDecimal spentAmount;
    private String pacingMode;

    public static BudgetResponse from(Budget budget) {
        return BudgetResponse.builder()
                .id(budget.getId())
                .campaignId(budget.getCampaign() != null ? budget.getCampaign().getId() : null)
                .totalBudget(budget.getTotalBudget())
                .dailyCap(budget.getDailyCap())
                .spentAmount(budget.getSpentAmount())
                .pacingMode(budget.getPacingMode())
                .build();
    }
}
