package com.adc.adcampaign.campaign.dto;

import com.adc.adcampaign.campaign.entity.TargetingRule;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TargetingRuleResponse {
    private Long id;
    private Long campaignId;
    private String ruleType;
    private String ruleValue;
    private String conditionType;

    public static TargetingRuleResponse from(TargetingRule rule) {
        return TargetingRuleResponse.builder()
                .id(rule.getId())
                .campaignId(rule.getCampaign() != null ? rule.getCampaign().getId() : null)
                .ruleType(rule.getRuleType())
                .ruleValue(rule.getRuleValue())
                .conditionType(rule.getConditionType())
                .build();
    }
}
