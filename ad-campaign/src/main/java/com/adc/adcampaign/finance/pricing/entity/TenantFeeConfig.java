package com.adc.adcampaign.finance.pricing.entity;

import com.adc.adcampaign.identity.entity.Tenant;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Per-tenant platform fee configuration. The fee policy is configured here,
 * not hard-coded in Billing, per
 * service/documents/03-domain-and-data-specification.md §5.
 */
@Entity
@Table(name = "tenant_fee_configs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantFeeConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false, unique = true)
    private Tenant tenant;

    @Column(name = "platform_fee_percent", nullable = false, precision = 6, scale = 4)
    private BigDecimal platformFeePercent;
}
