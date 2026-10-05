package com.adc.adcampaign.measurement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ViewabilityRequest {
    @NotNull
    private Long tenantId;

    @NotNull
    private Long campaignId;

    private Long creativeId;

    @NotBlank
    private String eventId;

    private Integer viewableDurationMs;

    private Integer viewablePercentage;
}
