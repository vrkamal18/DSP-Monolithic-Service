package com.adc.adcampaign.analytics;

import com.adc.adcampaign.campaign.entity.Advertiser;
import com.adc.adcampaign.campaign.entity.Campaign;
import com.adc.adcampaign.campaign.repository.CampaignRepository;
import com.adc.adcampaign.connectors.entity.Connector;
import com.adc.adcampaign.connectors.entity.ConnectorActivity;
import com.adc.adcampaign.connectors.entity.ConnectorActivityType;
import com.adc.adcampaign.connectors.entity.ConnectorStatus;
import com.adc.adcampaign.connectors.entity.ConnectorType;
import com.adc.adcampaign.connectors.repository.ConnectorActivityRepository;
import com.adc.adcampaign.connectors.repository.ConnectorRepository;
import com.adc.adcampaign.finance.billing.entity.LedgerEntry;
import com.adc.adcampaign.finance.billing.repository.LedgerEntryRepository;
import com.adc.adcampaign.foundation.error.NotFoundException;
import com.adc.adcampaign.measurement.entity.MeasurementEventType;
import com.adc.adcampaign.measurement.entity.VideoQuartile;
import com.adc.adcampaign.measurement.repository.MeasurementEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

class AnalyticsServiceTest {

    private static final Long TENANT_ID = 1L;
    private static final Long CAMPAIGN_ID = 10L;
    private static final Long ADVERTISER_ID = 5L;

    @Mock private CampaignRepository campaignRepository;
    @Mock private MeasurementEventRepository measurementEventRepository;
    @Mock private LedgerEntryRepository ledgerEntryRepository;
    @Mock private ConnectorRepository connectorRepository;
    @Mock private ConnectorActivityRepository connectorActivityRepository;

    private AnalyticsService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new AnalyticsService(campaignRepository, measurementEventRepository, ledgerEntryRepository,
                connectorRepository, connectorActivityRepository);
    }

    private Campaign campaignWithAdvertiser(Long id, Long advertiserId) {
        Advertiser advertiser = advertiserId != null ? Advertiser.builder().id(advertiserId).build() : null;
        return Campaign.builder().id(id).name("Fall Sale").advertiser(advertiser).build();
    }

    private void stubCounters(Long campaignId, long impressions, long clicks, long conversions,
                              BigDecimal conversionValue, long videoStarts, long videoCompletes,
                              BigDecimal spend) {
        when(measurementEventRepository.countByTenantIdAndCampaignIdAndEventType(
                TENANT_ID, campaignId, MeasurementEventType.IMPRESSION)).thenReturn(impressions);
        when(measurementEventRepository.countByTenantIdAndCampaignIdAndEventType(
                TENANT_ID, campaignId, MeasurementEventType.CLICK)).thenReturn(clicks);
        when(measurementEventRepository.countByTenantIdAndCampaignIdAndEventType(
                TENANT_ID, campaignId, MeasurementEventType.CONVERSION)).thenReturn(conversions);
        when(measurementEventRepository.sumConversionValueByTenantIdAndCampaignId(TENANT_ID, campaignId))
                .thenReturn(conversionValue);
        when(measurementEventRepository.countByTenantIdAndCampaignIdAndEventTypeAndVideoQuartile(
                TENANT_ID, campaignId, MeasurementEventType.VIDEO_QUARTILE, VideoQuartile.START))
                .thenReturn(videoStarts);
        when(measurementEventRepository.countByTenantIdAndCampaignIdAndEventTypeAndVideoQuartile(
                TENANT_ID, campaignId, MeasurementEventType.VIDEO_QUARTILE, VideoQuartile.COMPLETE))
                .thenReturn(videoCompletes);

        LedgerEntry entry = LedgerEntry.builder().tenantId(TENANT_ID).campaignId(campaignId).totalAmount(spend).build();
        when(ledgerEntryRepository.findByTenantIdAndCampaignId(TENANT_ID, campaignId))
                .thenReturn(spend == null ? List.of() : List.of(entry));
    }

    @Test
    void campaignMetricsComputesRatiosCorrectly() {
        when(campaignRepository.findByIdAndTenantId(CAMPAIGN_ID, TENANT_ID))
                .thenReturn(Optional.of(campaignWithAdvertiser(CAMPAIGN_ID, ADVERTISER_ID)));
        stubCounters(CAMPAIGN_ID, 1000, 100, 10, BigDecimal.valueOf(500), 50, 20, BigDecimal.valueOf(100));

        AnalyticsService.CampaignMetrics metrics = service.getCampaignMetrics(TENANT_ID, CAMPAIGN_ID);

        assertThat(metrics.campaignName()).isEqualTo("Fall Sale");
        assertThat(metrics.impressions()).isEqualTo(1000);
        assertThat(metrics.clicks()).isEqualTo(100);
        assertThat(metrics.conversions()).isEqualTo(10);
        assertThat(metrics.engagement()).isEqualTo(110);
        assertThat(metrics.ctr()).isEqualTo(10.0);
        assertThat(metrics.cvr()).isEqualTo(10.0);
        assertThat(metrics.vcr()).isEqualTo(40.0);
        assertThat(metrics.cpm()).isEqualByComparingTo("100.0000");
        assertThat(metrics.cpc()).isEqualByComparingTo("1.0000");
        assertThat(metrics.cpa()).isEqualByComparingTo("10.0000");
        assertThat(metrics.cpl()).isEqualByComparingTo(metrics.cpa());
        assertThat(metrics.roas()).isEqualTo(5.0);
        assertThat(metrics.roi()).isEqualTo(400.0);
    }

    @Test
    void campaignMetricsWithZeroDenominatorsReturnsZeroInsteadOfDividing() {
        when(campaignRepository.findByIdAndTenantId(CAMPAIGN_ID, TENANT_ID))
                .thenReturn(Optional.of(campaignWithAdvertiser(CAMPAIGN_ID, ADVERTISER_ID)));
        stubCounters(CAMPAIGN_ID, 0, 0, 0, null, 0, 0, null);

        AnalyticsService.CampaignMetrics metrics = service.getCampaignMetrics(TENANT_ID, CAMPAIGN_ID);

        assertThat(metrics.ctr()).isZero();
        assertThat(metrics.cvr()).isZero();
        assertThat(metrics.vcr()).isZero();
        assertThat(metrics.cpm()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(metrics.cpc()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(metrics.cpa()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(metrics.roas()).isZero();
        assertThat(metrics.roi()).isZero();
    }

    @Test
    void campaignMetricsThrowsNotFoundForUnknownOrCrossTenantCampaign() {
        when(campaignRepository.findByIdAndTenantId(CAMPAIGN_ID, TENANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCampaignMetrics(TENANT_ID, CAMPAIGN_ID))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void advertiserMetricsSumsOnlyThatAdvertisersCampaignsWithinTenant() {
        Campaign ownCampaign = campaignWithAdvertiser(1L, ADVERTISER_ID);
        Campaign otherAdvertiserCampaign = campaignWithAdvertiser(2L, 999L);
        when(campaignRepository.findByTenantId(TENANT_ID)).thenReturn(List.of(ownCampaign, otherAdvertiserCampaign));

        stubCounters(1L, 100, 10, 1, BigDecimal.valueOf(50), 5, 2, BigDecimal.valueOf(10));

        AnalyticsService.AdvertiserMetrics metrics = service.getAdvertiserMetrics(TENANT_ID, ADVERTISER_ID);

        assertThat(metrics.campaignCount()).isEqualTo(1);
        assertThat(metrics.impressions()).isEqualTo(100);
        assertThat(metrics.clicks()).isEqualTo(10);
        assertThat(metrics.conversions()).isEqualTo(1);
    }

    @Test
    void crossChannelSummaryCountsSuccessfulAndFailedSyncsPerConnector() {
        Connector connector = Connector.builder().id(7L).tenantId(TENANT_ID).type(ConnectorType.SOCIAL)
                .status(ConnectorStatus.CONNECTED).build();
        when(connectorRepository.findByTenantId(TENANT_ID)).thenReturn(List.of(connector));
        when(connectorActivityRepository.findByTenantIdAndConnectorId(TENANT_ID, 7L)).thenReturn(List.of(
                ConnectorActivity.builder().activityType(ConnectorActivityType.SYNC).success(true).build(),
                ConnectorActivity.builder().activityType(ConnectorActivityType.SYNC).success(true).build(),
                ConnectorActivity.builder().activityType(ConnectorActivityType.SYNC).success(false).build(),
                ConnectorActivity.builder().activityType(ConnectorActivityType.CONNECT).success(true).build()));

        List<AnalyticsService.ChannelSyncSummary> summaries = service.getCrossChannelSummary(TENANT_ID);

        assertThat(summaries).hasSize(1);
        AnalyticsService.ChannelSyncSummary summary = summaries.get(0);
        assertThat(summary.totalSyncs()).isEqualTo(3);
        assertThat(summary.successfulSyncs()).isEqualTo(2);
        assertThat(summary.failedSyncs()).isEqualTo(1);
    }
}
