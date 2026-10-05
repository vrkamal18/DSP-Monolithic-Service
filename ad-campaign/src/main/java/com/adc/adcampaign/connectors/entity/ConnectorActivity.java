package com.adc.adcampaign.connectors.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A generic activity/history log covering every connector action —
 * "synchronization job history" from
 * service/documents/02-module-specifications.md §11, generalized to also
 * cover connect/disconnect/test/push/pause/resume rather than modeling a
 * near-duplicate table per action type.
 */
@Entity
@Table(name = "connector_activities")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConnectorActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "connector_id", nullable = false)
    private Long connectorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false)
    private ConnectorActivityType activityType;

    @Column(nullable = false)
    private boolean success;

    private String details;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void setCreatedAt() {
        this.createdAt = LocalDateTime.now();
    }
}
