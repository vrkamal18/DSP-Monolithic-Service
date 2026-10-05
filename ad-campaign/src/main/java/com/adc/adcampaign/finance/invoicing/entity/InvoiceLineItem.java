package com.adc.adcampaign.finance.invoicing.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * One campaign's subtotal for an invoice's period — not one row per
 * LedgerEntry, which would be unusably granular for a high-volume campaign.
 * The description snapshots the campaign name at generation time so a later
 * rename doesn't rewrite invoice history.
 */
@Entity
@Table(name = "invoice_line_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_id", nullable = false)
    private Long invoiceId;

    @Column(name = "campaign_id")
    private Long campaignId;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false, precision = 14, scale = 4)
    private BigDecimal amount;
}
