package com.adc.adcampaign.analytics.dto;

import com.adc.adcampaign.analytics.AnalyticsService;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CampaignAnalyticsResponse {
    private Long campaignId;
    private String campaignName;
    private long impressions;
    private long clicks;
    private long conversions;
    private long engagement;
    private BigDecimal conversionValue;
    private BigDecimal spend;
    private double ctr;
    private double cvr;
    private double vcr;
    private BigDecimal cpm;
    private BigDecimal cpc;
    private BigDecimal cpa;
    private BigDecimal cpe;
    private BigDecimal cpl;
    private double roas;
    private double roi;

    public static CampaignAnalyticsResponse from(AnalyticsService.CampaignMetrics m) {
        return CampaignAnalyticsResponse.builder()
                .campaignId(m.campaignId())
                .campaignName(m.campaignName())
                .impressions(m.impressions())
                .clicks(m.clicks())
                .conversions(m.conversions())
                .engagement(m.engagement())
                .conversionValue(m.conversionValue())
                .spend(m.spend())
                .ctr(m.ctr())
                .cvr(m.cvr())
                .vcr(m.vcr())
                .cpm(m.cpm())
                .cpc(m.cpc())
                .cpa(m.cpa())
                .cpe(m.cpe())
                .cpl(m.cpl())
                .roas(m.roas())
                .roi(m.roi())
                .build();
    }
}
