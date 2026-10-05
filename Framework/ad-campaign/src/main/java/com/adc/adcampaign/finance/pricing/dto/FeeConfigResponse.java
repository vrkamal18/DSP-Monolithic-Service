package com.adc.adcampaign.finance.pricing.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class FeeConfigResponse {
    private BigDecimal platformFeePercent;
}
