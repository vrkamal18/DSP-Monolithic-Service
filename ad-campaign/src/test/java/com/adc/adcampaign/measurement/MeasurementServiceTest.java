package com.adc.adcampaign.measurement;

import com.adc.adcampaign.measurement.entity.MeasurementEventType;
import com.adc.adcampaign.measurement.entity.VideoQuartile;
import com.adc.adcampaign.measurement.repository.MeasurementEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class MeasurementServiceTest {

    private static final Long TENANT_ID = 1L;
    private static final Long CAMPAIGN_ID = 10L;
    private static final Long CREATIVE_ID = 20L;

    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private MeasurementEventRepository measurementEventRepository;

    private MeasurementService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new MeasurementService(jdbcTemplate, measurementEventRepository);
    }

    @SuppressWarnings("unchecked")
    private void stubInsertResult(Long firstReturn, Long... subsequentReturns) {
        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(firstReturn, subsequentReturns);
    }

    @Test
    void recordImpressionIsIdempotentOnDuplicateEventId() {
        stubInsertResult(1L, (Long) null);

        boolean first = service.recordImpression(TENANT_ID, CAMPAIGN_ID, CREATIVE_ID, "evt-1");
        boolean second = service.recordImpression(TENANT_ID, CAMPAIGN_ID, CREATIVE_ID, "evt-1");

        assertThat(first).isTrue();
        assertThat(second).isFalse();
    }

    @Test
    void recordClickIsIdempotentOnDuplicateEventId() {
        stubInsertResult(2L, (Long) null);

        boolean first = service.recordClick(TENANT_ID, CAMPAIGN_ID, CREATIVE_ID, "evt-2", "https://example.com");
        boolean second = service.recordClick(TENANT_ID, CAMPAIGN_ID, CREATIVE_ID, "evt-2", "https://example.com");

        assertThat(first).isTrue();
        assertThat(second).isFalse();
    }

    @Test
    void recordConversionIsIdempotentOnDuplicateEventId() {
        stubInsertResult(3L, (Long) null);

        boolean first = service.recordConversion(TENANT_ID, CAMPAIGN_ID, CREATIVE_ID, "evt-3", "purchase", BigDecimal.TEN);
        boolean second = service.recordConversion(TENANT_ID, CAMPAIGN_ID, CREATIVE_ID, "evt-3", "purchase", BigDecimal.TEN);

        assertThat(first).isTrue();
        assertThat(second).isFalse();
    }

    @Test
    void recordViewabilityIsIdempotentOnDuplicateEventId() {
        stubInsertResult(4L, (Long) null);

        boolean first = service.recordViewability(TENANT_ID, CAMPAIGN_ID, CREATIVE_ID, "evt-4", 1000, 60);
        boolean second = service.recordViewability(TENANT_ID, CAMPAIGN_ID, CREATIVE_ID, "evt-4", 1000, 60);

        assertThat(first).isTrue();
        assertThat(second).isFalse();
    }

    @Test
    void recordVideoQuartileIsIdempotentOnDuplicateEventId() {
        stubInsertResult(5L, (Long) null);

        boolean first = service.recordVideoQuartile(TENANT_ID, CAMPAIGN_ID, CREATIVE_ID, "evt-5", VideoQuartile.MIDPOINT);
        boolean second = service.recordVideoQuartile(TENANT_ID, CAMPAIGN_ID, CREATIVE_ID, "evt-5", VideoQuartile.MIDPOINT);

        assertThat(first).isTrue();
        assertThat(second).isFalse();
    }

    @Test
    void getSummaryReturnsCountsPerEventTypeScopedToTenantAndCampaign() {
        when(measurementEventRepository.countByTenantIdAndCampaignIdAndEventType(
                TENANT_ID, CAMPAIGN_ID, MeasurementEventType.IMPRESSION)).thenReturn(100L);
        when(measurementEventRepository.countByTenantIdAndCampaignIdAndEventType(
                TENANT_ID, CAMPAIGN_ID, MeasurementEventType.CLICK)).thenReturn(10L);
        when(measurementEventRepository.countByTenantIdAndCampaignIdAndEventType(
                TENANT_ID, CAMPAIGN_ID, MeasurementEventType.CONVERSION)).thenReturn(2L);
        when(measurementEventRepository.countByTenantIdAndCampaignIdAndEventType(
                TENANT_ID, CAMPAIGN_ID, MeasurementEventType.VIEWABILITY)).thenReturn(50L);
        when(measurementEventRepository.countByTenantIdAndCampaignIdAndEventType(
                TENANT_ID, CAMPAIGN_ID, MeasurementEventType.VIDEO_QUARTILE)).thenReturn(20L);

        MeasurementService.MeasurementSummary summary = service.getSummary(TENANT_ID, CAMPAIGN_ID);

        assertThat(summary.impressions()).isEqualTo(100L);
        assertThat(summary.clicks()).isEqualTo(10L);
        assertThat(summary.conversions()).isEqualTo(2L);
        assertThat(summary.viewabilityEvents()).isEqualTo(50L);
        assertThat(summary.videoQuartileEvents()).isEqualTo(20L);
    }
}
