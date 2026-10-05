package com.adc.adcampaign.finance.invoicing.dto;

import com.adc.adcampaign.finance.invoicing.entity.InvoiceLineItem;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class InvoiceLineItemResponse {
    private Long campaignId;
    private String description;
    private BigDecimal amount;

    public static InvoiceLineItemResponse from(InvoiceLineItem item) {
        return InvoiceLineItemResponse.builder()
                .campaignId(item.getCampaignId())
                .description(item.getDescription())
                .amount(item.getAmount())
                .build();
    }
}
