package com.adc.adcampaign.finance.payments.dto;

import com.adc.adcampaign.finance.payments.entity.Transaction;
import com.adc.adcampaign.finance.payments.entity.TransactionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TransactionResponse {
    private Long id;
    private TransactionType type;
    private BigDecimal amount;
    private String currency;
    private String providerChargeId;
    private LocalDateTime createdAt;

    public static TransactionResponse from(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .type(transaction.getType())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .providerChargeId(transaction.getProviderChargeId())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
