package com.adc.adcampaign.finance.invoicing.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A snapshot of billable facts for one advertiser over one period — per
 * service/documents/03-domain-and-data-specification.md §2, "Snapshot of
 * billable facts at issue time." No PDF bytes are stored here; the document
 * is regenerated on demand from this row plus its InvoiceLineItems, which
 * are themselves frozen at generation time.
 */
@Entity
@Table(name = "invoices", uniqueConstraints = @UniqueConstraint(
        name = "uk_invoice_tenant_advertiser_period",
        columnNames = {"tenant_id", "advertiser_id", "period_start", "period_end"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "advertiser_id", nullable = false)
    private Long advertiserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 4)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private String currency;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime issuedAt;
    private LocalDateTime sentAt;
    private LocalDateTime paidAt;
    private LocalDateTime voidedAt;

    @PrePersist
    public void setCreatedAt() {
        this.createdAt = LocalDateTime.now();
    }
}
