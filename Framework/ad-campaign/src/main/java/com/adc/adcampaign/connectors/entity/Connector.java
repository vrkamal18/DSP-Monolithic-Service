package com.adc.adcampaign.connectors.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A tenant's connection to an external platform. {@code credentialRef} is
 * the opaque token/API-key the client supplied on connect — accepted on
 * write, never included in any response DTO, per
 * service/documents/06-security-and-multitenancy.md §8 ("Connector
 * credential read APIs return metadata, never the complete secret").
 */
@Entity
@Table(name = "connectors", uniqueConstraints = @UniqueConstraint(
        name = "uk_connector_tenant_type", columnNames = {"tenant_id", "type"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Connector {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConnectorType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConnectorStatus status;

    @Column(name = "external_account_id")
    private String externalAccountId;

    @Column(name = "credential_ref")
    private String credentialRef;

    private LocalDateTime connectedAt;
    private LocalDateTime lastSyncedAt;
    private LocalDateTime lastTestedAt;
    private Boolean lastTestSuccessful;
}
