package com.adc.adcampaign.measurement.dto;

import com.adc.adcampaign.measurement.entity.VideoQuartile;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VideoQuartileRequest {
    @NotNull
    private Long tenantId;

    @NotNull
    private Long campaignId;

    private Long creativeId;

    @NotBlank
    private String eventId;

    @NotNull
    private VideoQuartile quartile;
}
