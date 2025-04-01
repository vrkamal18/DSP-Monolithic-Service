package com.adc.adcampaign.campaign.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CampaignReportDTO {
    private Long campaignId;
    private String campaignName;

    private long totalImpressions;
    private long totalClicks;
    private double ctr; // click-through rate (%)

    private double totalBudget;
    private double spentAmount;
    private double remainingBudget;
}

