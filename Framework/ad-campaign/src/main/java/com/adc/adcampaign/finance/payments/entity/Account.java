package com.adc.adcampaign.finance.payments.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * An advertiser's funding account. Owned by Finance/Payments, distinct from
 * the per-campaign charge ledger in finance.billing — see
 * service/documents/03-domain-and-data-specification.md §2.
 */
@Entity
@Table(name = "accounts", uniqueConstraints = @UniqueConstraint(
        name = "uk_account_tenant_advertiser", columnNames = {"tenant_id", "advertiser_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "advertiser_id", nullable = false)
    private Long advertiserId;

    @Column(nullable = false, precision = 14, scale = 4)
    private BigDecimal balance;

    @Column(nullable = false)
    private String currency;
}
