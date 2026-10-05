package com.adc.adcampaign.campaign.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreativeRequest {
    @NotNull
    private Long campaignId;

    private String name;
    private String type;
    private String url;
    private Integer width;
    private Integer height;
}
