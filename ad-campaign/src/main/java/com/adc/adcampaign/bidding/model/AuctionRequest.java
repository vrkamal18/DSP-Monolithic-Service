package com.adc.adcampaign.bidding.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Normalized, vendor-neutral view of an incoming auction opportunity. The
 * bidding module consumes this instead of raw OpenRTB DTOs so the exchange
 * adapter and wire format can change without touching decisioning logic.
 */
@Data
@Builder
public class AuctionRequest {
    private String requestId;
    private String exchange;
    private Long tenantId;
    private List<ImpressionOpportunity> impressions;
    private Long tmaxMillis;
    private List<String> currencies;
    private String userId;
    private String deviceType;
    private String geoCountry;

    /** Absolute deadline (System.nanoTime() domain) for the whole decision pipeline. */
    private long deadlineNanos;
}
