package com.adc.adcampaign.measurement.dto;

import com.adc.adcampaign.measurement.MeasurementService;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MeasurementSummaryResponse {
    private Long campaignId;
    private long impressions;
    private long clicks;
    private long conversions;
    private long viewabilityEvents;
    private long videoQuartileEvents;

    public static MeasurementSummaryResponse from(Long campaignId, MeasurementService.MeasurementSummary summary) {
        return MeasurementSummaryResponse.builder()
                .campaignId(campaignId)
                .impressions(summary.impressions())
                .clicks(summary.clicks())
                .conversions(summary.conversions())
                .viewabilityEvents(summary.viewabilityEvents())
                .videoQuartileEvents(summary.videoQuartileEvents())
                .build();
    }
}
