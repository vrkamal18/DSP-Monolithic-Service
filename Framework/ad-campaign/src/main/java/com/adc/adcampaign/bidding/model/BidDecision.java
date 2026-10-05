package com.adc.adcampaign.bidding.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class BidDecision {
    private String impId;
    private Long campaignId;
    private Long creativeId;
    private BigDecimal price;
    private String adMarkup;
    private int width;
    private int height;

    /** requestId:impId — the same key used as the billing idempotency key, reused to correlate a later win notice. */
    private String sourceEventId;
}
