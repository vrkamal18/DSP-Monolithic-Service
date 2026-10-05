package com.adc.adcampaign.finance.billing.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Append-only financial record, per
 * service/documents/03-domain-and-data-specification.md §2/§8 — corrections
 * use a separate ADJUSTMENT entry, never an edit of a CHARGE. The unique
 * (tenant_id, source_event_id) constraint is the idempotency mechanism:
 * processing the same bid win (or the same client-supplied Idempotency-Key
 * for an adjustment) twice inserts nothing the second time.
 */
@Entity
@Table(name = "ledger_entries", uniqueConstraints = @UniqueConstraint(
        name = "uk_ledger_entry_source_event", columnNames = {"tenant_id", "source_event_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "campaign_id")
    private Long campaignId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false)
    private LedgerEntryType entryType;

    @Column(name = "source_event_id", nullable = false)
    private String sourceEventId;

    @Column(name = "media_cost", precision = 14, scale = 4)
    private BigDecimal mediaCost;

    @Column(name = "platform_fee", precision = 14, scale = 4)
    private BigDecimal platformFee;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 4)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private String currency;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void setCreatedAt() {
        this.createdAt = LocalDateTime.now();
    }
}
