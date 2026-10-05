package com.adc.adcampaign.measurement;

import com.adc.adcampaign.measurement.entity.MeasurementEventType;
import com.adc.adcampaign.measurement.entity.VideoQuartile;
import com.adc.adcampaign.measurement.repository.MeasurementEventRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * Records validated measurement events idempotently. See
 * service/documents/02-module-specifications.md §12 and the measurement
 * plan for why writes trust caller-supplied tenant/campaign/creative ids
 * rather than looking them up (Measurement has no dependency on campaign/
 * creative contracts).
 */
@Service
public class MeasurementService {

    private static final String INSERT_EVENT_SQL = """
            INSERT INTO measurement_events
                (tenant_id, campaign_id, creative_id, event_type, event_id, destination_url,
                 conversion_type, conversion_value, viewable_duration_ms, viewable_percentage,
                 video_quartile, recorded_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (tenant_id, event_id) DO NOTHING
            RETURNING id
            """;

    private final JdbcTemplate jdbcTemplate;
    private final MeasurementEventRepository measurementEventRepository;

    public MeasurementService(JdbcTemplate jdbcTemplate, MeasurementEventRepository measurementEventRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.measurementEventRepository = measurementEventRepository;
    }

    @Transactional
    public boolean recordImpression(Long tenantId, Long campaignId, Long creativeId, String eventId) {
        return insert(tenantId, campaignId, creativeId, MeasurementEventType.IMPRESSION, eventId,
                null, null, null, null, null, null);
    }

    @Transactional
    public boolean recordClick(Long tenantId, Long campaignId, Long creativeId, String eventId, String destinationUrl) {
        return insert(tenantId, campaignId, creativeId, MeasurementEventType.CLICK, eventId,
                destinationUrl, null, null, null, null, null);
    }

    @Transactional
    public boolean recordConversion(Long tenantId, Long campaignId, Long creativeId, String eventId,
                                     String conversionType, BigDecimal conversionValue) {
        return insert(tenantId, campaignId, creativeId, MeasurementEventType.CONVERSION, eventId,
                null, conversionType, conversionValue, null, null, null);
    }

    @Transactional
    public boolean recordViewability(Long tenantId, Long campaignId, Long creativeId, String eventId,
                                      Integer viewableDurationMs, Integer viewablePercentage) {
        return insert(tenantId, campaignId, creativeId, MeasurementEventType.VIEWABILITY, eventId,
                null, null, null, viewableDurationMs, viewablePercentage, null);
    }

    @Transactional
    public boolean recordVideoQuartile(Long tenantId, Long campaignId, Long creativeId, String eventId,
                                        VideoQuartile quartile) {
        return insert(tenantId, campaignId, creativeId, MeasurementEventType.VIDEO_QUARTILE, eventId,
                null, null, null, null, null, quartile);
    }

    public MeasurementSummary getSummary(Long tenantId, Long campaignId) {
        return new MeasurementSummary(
                measurementEventRepository.countByTenantIdAndCampaignIdAndEventType(
                        tenantId, campaignId, MeasurementEventType.IMPRESSION),
                measurementEventRepository.countByTenantIdAndCampaignIdAndEventType(
                        tenantId, campaignId, MeasurementEventType.CLICK),
                measurementEventRepository.countByTenantIdAndCampaignIdAndEventType(
                        tenantId, campaignId, MeasurementEventType.CONVERSION),
                measurementEventRepository.countByTenantIdAndCampaignIdAndEventType(
                        tenantId, campaignId, MeasurementEventType.VIEWABILITY),
                measurementEventRepository.countByTenantIdAndCampaignIdAndEventType(
                        tenantId, campaignId, MeasurementEventType.VIDEO_QUARTILE));
    }

    private boolean insert(Long tenantId, Long campaignId, Long creativeId, MeasurementEventType type,
                            String eventId, String destinationUrl, String conversionType,
                            BigDecimal conversionValue, Integer viewableDurationMs, Integer viewablePercentage,
                            VideoQuartile quartile) {
        Long insertedId = jdbcTemplate.query(INSERT_EVENT_SQL,
                rs -> rs.next() ? rs.getLong(1) : null,
                tenantId, campaignId, creativeId, type.name(), eventId, destinationUrl,
                conversionType, conversionValue, viewableDurationMs, viewablePercentage,
                quartile != null ? quartile.name() : null, Timestamp.valueOf(LocalDateTime.now()));
        return insertedId != null;
    }

    public record MeasurementSummary(long impressions, long clicks, long conversions,
                                      long viewabilityEvents, long videoQuartileEvents) {
    }
}
