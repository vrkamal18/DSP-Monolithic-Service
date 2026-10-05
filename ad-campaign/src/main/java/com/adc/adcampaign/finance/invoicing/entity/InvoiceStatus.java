package com.adc.adcampaign.finance.invoicing.entity;

/**
 * service/documents/03-domain-and-data-specification.md §7: DRAFT -> ISSUED
 * -> SENT -> PAID, ISSUED -> VOID. DRAFT -> VOID is an accepted addition —
 * discarding a wrongly-generated draft shouldn't require issuing it first.
 */
public enum InvoiceStatus {
    DRAFT,
    ISSUED,
    SENT,
    PAID,
    VOID
}
