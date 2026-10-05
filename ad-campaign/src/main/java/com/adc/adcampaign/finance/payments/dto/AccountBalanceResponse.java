package com.adc.adcampaign.finance.payments.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class AccountBalanceResponse {
    private Long advertiserId;
    private BigDecimal balance;
    private String currency;
}
