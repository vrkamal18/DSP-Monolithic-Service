package com.adc.adcampaign.bidding.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ImpressionOpportunity {
    private String impId;
    private Integer width;
    private Integer height;
    private BigDecimal bidFloor;
    private String bidFloorCurrency;
}
