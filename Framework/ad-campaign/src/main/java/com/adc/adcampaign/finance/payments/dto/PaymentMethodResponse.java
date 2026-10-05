package com.adc.adcampaign.finance.payments.dto;

import com.adc.adcampaign.finance.payments.entity.PaymentMethod;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PaymentMethodResponse {
    private Long id;
    private Long advertiserId;
    private String provider;
    private String brand;
    private String last4;
    private Integer expiryMonth;
    private Integer expiryYear;
    private LocalDateTime createdAt;

    public static PaymentMethodResponse from(PaymentMethod method) {
        return PaymentMethodResponse.builder()
                .id(method.getId())
                .advertiserId(method.getAdvertiserId())
                .provider(method.getProvider())
                .brand(method.getBrand())
                .last4(method.getLast4())
                .expiryMonth(method.getExpiryMonth())
                .expiryYear(method.getExpiryYear())
                .createdAt(method.getCreatedAt())
                .build();
    }
}
