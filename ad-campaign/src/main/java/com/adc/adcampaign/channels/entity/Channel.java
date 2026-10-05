package com.adc.adcampaign.channels.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Per-tenant channel definition/enabled state
 * (service/documents/02-module-specifications.md §10). A type with no row
 * is disabled by default — same "unconfigured means off" convention
 * finance.pricing.FeeCalculationService already uses.
 */
@Entity
@Table(name = "channels", uniqueConstraints = @UniqueConstraint(
        name = "uk_channel_tenant_type", columnNames = {"tenant_id", "type"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Channel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChannelType type;

    @Column(nullable = false)
    private boolean enabled;
}
