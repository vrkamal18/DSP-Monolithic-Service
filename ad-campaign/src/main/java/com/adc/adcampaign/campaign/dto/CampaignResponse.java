package com.adc.adcampaign.campaign.dto;

import com.adc.adcampaign.campaign.entity.Campaign;
import com.adc.adcampaign.campaign.entity.CampaignStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class CampaignResponse {
    private Long id;
    private String name;
    private BigDecimal bidPrice;
    private String objective;
    private Long advertiserId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private CampaignStatus status;
    private LocalDateTime createdAt;

    public static CampaignResponse from(Campaign campaign) {
        return CampaignResponse.builder()
                .id(campaign.getId())
                .name(campaign.getName())
                .bidPrice(campaign.getBidPrice())
                .objective(campaign.getObjective())
                .advertiserId(campaign.getAdvertiser() != null ? campaign.getAdvertiser().getId() : null)
                .startDate(campaign.getStartDate())
                .endDate(campaign.getEndDate())
                .status(campaign.getStatus())
                .createdAt(campaign.getCreatedAt())
                .build();
    }
}
