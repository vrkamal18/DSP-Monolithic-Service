package com.adc.adcampaign.campaign.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TargetingRuleRequest {
    @NotNull
    private Long campaignId;

    private String ruleType;
    private String ruleValue;
    private String conditionType;
}
