package com.adc.adcampaign.campaign.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Append-only audit trail for campaign status transitions, per
 * service/documents/02-module-specifications.md §4 ("Campaign state changes
 * are audited"). Write-only for now — no read API yet.
 */
@Entity
@Table(name = "campaign_status_changes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampaignStatusChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status")
    private CampaignStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false)
    private CampaignStatus toStatus;

    @Column(name = "changed_by")
    private Long changedBy;

    @Column(name = "changed_at", updatable = false)
    private LocalDateTime changedAt;

    @PrePersist
    public void setChangedAt() {
        this.changedAt = LocalDateTime.now();
    }
}
