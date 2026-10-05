package com.adc.adcampaign.finance.payments.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentMethodRequest {
    @NotNull
    private Long advertiserId;

    /** Defaults to "MOCK" — no real provider has been chosen yet. */
    private String provider;

    @NotBlank
    private String token;

    private String brand;
    private String last4;
    private Integer expiryMonth;
    private Integer expiryYear;
}
