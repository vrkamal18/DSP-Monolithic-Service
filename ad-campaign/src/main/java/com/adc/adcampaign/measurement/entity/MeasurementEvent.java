package com.adc.adcampaign.measurement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A single validated tracking event (impression/click/conversion/viewability/
 * video quartile). {@code eventId} is a caller-supplied idempotency key —
 * duplicate pixel fires (browser retries, double-loads) must not double
 * count, mirroring the {@code source_event_id} pattern already used for
 * billing ledger entries. Written from public, unauthenticated {@code /t/**}
 * endpoints, so {@code tenantId}/{@code campaignId}/{@code creativeId} are
 * trusted only as opaque identifiers, never cross-checked against Campaign/
 * Creative — Measurement has no dependency on those modules per
 * service/documents/01-service-architecture.md §6.
 */
@Entity
@Table(name = "measurement_events", uniqueConstraints = @UniqueConstraint(
        name = "uk_measurement_event_tenant_event", columnNames = {"tenant_id", "event_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeasurementEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;

    @Column(name = "creative_id")
    private Long creativeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private MeasurementEventType eventType;

    @Column(name = "event_id", nullable = false)
    private String eventId;

    @Column(name = "destination_url")
    private String destinationUrl;

    @Column(name = "conversion_type")
    private String conversionType;

    @Column(name = "conversion_value", precision = 12, scale = 4)
    private BigDecimal conversionValue;

    @Column(name = "viewable_duration_ms")
    private Integer viewableDurationMs;

    @Column(name = "viewable_percentage")
    private Integer viewablePercentage;

    @Enumerated(EnumType.STRING)
    @Column(name = "video_quartile")
    private VideoQuartile videoQuartile;

    @Column(name = "recorded_at", updatable = false)
    private LocalDateTime recordedAt;

    @PrePersist
    public void setRecordedAt() {
        this.recordedAt = LocalDateTime.now();
    }
}
