package com.adc.adcampaign.connectors.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * The external-to-internal identifier mapping
 * service/documents/02-module-specifications.md §11 calls out — created
 * once a campaign is pushed to a connector, looked up again for
 * pause/resume.
 */
@Entity
@Table(name = "external_id_mappings", uniqueConstraints = @UniqueConstraint(
        name = "uk_external_id_mapping", columnNames = {"tenant_id", "connector_id", "campaign_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExternalIdMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "connector_id", nullable = false)
    private Long connectorId;

    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;

    @Column(name = "external_campaign_id", nullable = false)
    private String externalCampaignId;
}
