package com.adc.adcampaign.campaign.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CampaignRequest {
    @NotBlank
    private String name;

    private BigDecimal bidPrice;
    private String objective;
    private Long advertiserId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
