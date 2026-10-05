package com.adc.adcampaign.finance.invoicing.dto;

import com.adc.adcampaign.finance.invoicing.entity.Invoice;
import com.adc.adcampaign.finance.invoicing.entity.InvoiceLineItem;
import com.adc.adcampaign.finance.invoicing.entity.InvoiceStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class InvoiceResponse {
    private Long id;
    private Long advertiserId;
    private InvoiceStatus status;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal totalAmount;
    private String currency;
    private LocalDateTime issuedAt;
    private LocalDateTime sentAt;
    private LocalDateTime paidAt;
    private LocalDateTime voidedAt;
    private List<InvoiceLineItemResponse> lineItems;

    public static InvoiceResponse from(Invoice invoice, List<InvoiceLineItem> lineItems) {
        return InvoiceResponse.builder()
                .id(invoice.getId())
                .advertiserId(invoice.getAdvertiserId())
                .status(invoice.getStatus())
                .periodStart(invoice.getPeriodStart())
                .periodEnd(invoice.getPeriodEnd())
                .totalAmount(invoice.getTotalAmount())
                .currency(invoice.getCurrency())
                .issuedAt(invoice.getIssuedAt())
                .sentAt(invoice.getSentAt())
                .paidAt(invoice.getPaidAt())
                .voidedAt(invoice.getVoidedAt())
                .lineItems(lineItems.stream().map(InvoiceLineItemResponse::from).toList())
                .build();
    }
}
