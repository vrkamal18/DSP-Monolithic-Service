package com.adc.adcampaign.finance.billing.dto;

import com.adc.adcampaign.finance.billing.entity.LedgerEntry;
import com.adc.adcampaign.finance.billing.entity.LedgerEntryType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class LedgerEntryResponse {
    private Long id;
    private Long campaignId;
    private LedgerEntryType entryType;
    private String sourceEventId;
    private BigDecimal mediaCost;
    private BigDecimal platformFee;
    private BigDecimal totalAmount;
    private String currency;
    private LocalDateTime createdAt;

    public static LedgerEntryResponse from(LedgerEntry entry) {
        return LedgerEntryResponse.builder()
                .id(entry.getId())
                .campaignId(entry.getCampaignId())
                .entryType(entry.getEntryType())
                .sourceEventId(entry.getSourceEventId())
                .mediaCost(entry.getMediaCost())
                .platformFee(entry.getPlatformFee())
                .totalAmount(entry.getTotalAmount())
                .currency(entry.getCurrency())
                .createdAt(entry.getCreatedAt())
                .build();
    }
}
