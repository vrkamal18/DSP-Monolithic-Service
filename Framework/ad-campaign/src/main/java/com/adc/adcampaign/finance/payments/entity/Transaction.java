package com.adc.adcampaign.finance.payments.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Append-only record of money moving in/out of an advertiser's account.
 * Idempotent on (tenant_id, source_event_id) — the same pattern already
 * proven in finance.billing.LedgerEntry.
 */
@Entity
@Table(name = "payment_transactions", uniqueConstraints = @UniqueConstraint(
        name = "uk_transaction_source_event", columnNames = {"tenant_id", "source_event_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "advertiser_id", nullable = false)
    private Long advertiserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Column(nullable = false, precision = 14, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Column(name = "source_event_id", nullable = false)
    private String sourceEventId;

    @Column(name = "provider_charge_id")
    private String providerChargeId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void setCreatedAt() {
        this.createdAt = LocalDateTime.now();
    }
}
