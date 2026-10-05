package com.adc.adcampaign.analytics;

import com.adc.adcampaign.campaign.entity.Campaign;
import com.adc.adcampaign.campaign.repository.CampaignRepository;
import com.adc.adcampaign.connectors.entity.Connector;
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
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Computes campaign/advertiser performance and connector sync health on
 * demand from Measurement, Billing, and Connectors — see
 * service/documents/02-module-specifications.md §14 and the analytics plan
 * for why this is on-demand rather than a materialized projection (no event
 * pipeline exists in this codebase yet).
 */
@Service
public class AnalyticsService {

    private static final int SCALE = 4;

    private final CampaignRepository campaignRepository;
    private final MeasurementEventRepository measurementEventRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final ConnectorRepository connectorRepository;
    private final ConnectorActivityRepository connectorActivityRepository;

    public AnalyticsService(CampaignRepository campaignRepository,
                             MeasurementEventRepository measurementEventRepository,
                             LedgerEntryRepository ledgerEntryRepository,
                             ConnectorRepository connectorRepository,
                             ConnectorActivityRepository connectorActivityRepository) {
        this.campaignRepository = campaignRepository;
        this.measurementEventRepository = measurementEventRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.connectorRepository = connectorRepository;
        this.connectorActivityRepository = connectorActivityRepository;
    }

    public CampaignMetrics getCampaignMetrics(Long tenantId, Long campaignId) {
        Campaign campaign = campaignRepository.findByIdAndTenantId(campaignId, tenantId)
                .orElseThrow(() -> new NotFoundException("Campaign not found: " + campaignId));
        RawCounters raw = fetchRawCounters(tenantId, campaignId);
        return toCampaignMetrics(campaignId, campaign.getName(), raw);
    }

    public AdvertiserMetrics getAdvertiserMetrics(Long tenantId, Long advertiserId) {
        List<Campaign> campaigns = campaignRepository.findByTenantId(tenantId).stream()
                .filter(c -> c.getAdvertiser() != null && advertiserId.equals(c.getAdvertiser().getId()))
                .toList();

        RawCounters total = RawCounters.zero();
        for (Campaign campaign : campaigns) {
            total = total.plus(fetchRawCounters(tenantId, campaign.getId()));
        }
        return toAdvertiserMetrics(advertiserId, campaigns.size(), total);
    }

    public List<ChannelSyncSummary> getCrossChannelSummary(Long tenantId) {
        return connectorRepository.findByTenantId(tenantId).stream()
                .map(connector -> toChannelSyncSummary(tenantId, connector))
                .toList();
    }

    private ChannelSyncSummary toChannelSyncSummary(Long tenantId, Connector connector) {
        long totalSyncs = 0;
        long successfulSyncs = 0;
        for (var activity : connectorActivityRepository.findByTenantIdAndConnectorId(tenantId, connector.getId())) {
            if (activity.getActivityType() != ConnectorActivityType.SYNC) {
                continue;
            }
            totalSyncs++;
            if (activity.isSuccess()) {
                successfulSyncs++;
            }
        }

        return new ChannelSyncSummary(connector.getId(), connector.getType(), connector.getStatus(),
                totalSyncs, successfulSyncs, totalSyncs - successfulSyncs, connector.getLastSyncedAt());
    }

    private RawCounters fetchRawCounters(Long tenantId, Long campaignId) {
        long impressions = measurementEventRepository.countByTenantIdAndCampaignIdAndEventType(
                tenantId, campaignId, MeasurementEventType.IMPRESSION);
        long clicks = measurementEventRepository.countByTenantIdAndCampaignIdAndEventType(
                tenantId, campaignId, MeasurementEventType.CLICK);
        long conversions = measurementEventRepository.countByTenantIdAndCampaignIdAndEventType(
                tenantId, campaignId, MeasurementEventType.CONVERSION);
        BigDecimal conversionValue = nullSafe(
                measurementEventRepository.sumConversionValueByTenantIdAndCampaignId(tenantId, campaignId));
        long videoStarts = measurementEventRepository.countByTenantIdAndCampaignIdAndEventTypeAndVideoQuartile(
                tenantId, campaignId, MeasurementEventType.VIDEO_QUARTILE, VideoQuartile.START);
        long videoCompletes = measurementEventRepository.countByTenantIdAndCampaignIdAndEventTypeAndVideoQuartile(
                tenantId, campaignId, MeasurementEventType.VIDEO_QUARTILE, VideoQuartile.COMPLETE);

        BigDecimal spend = ledgerEntryRepository.findByTenantIdAndCampaignId(tenantId, campaignId).stream()
                .map(LedgerEntry::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new RawCounters(impressions, clicks, conversions, conversionValue, spend, videoStarts, videoCompletes);
    }

    private CampaignMetrics toCampaignMetrics(Long campaignId, String campaignName, RawCounters raw) {
        Ratios ratios = computeRatios(raw);
        return new CampaignMetrics(campaignId, campaignName, raw.impressions, raw.clicks, raw.conversions,
                raw.clicks + raw.conversions, raw.conversionValue, raw.spend,
                ratios.ctr, ratios.cvr, ratios.vcr, ratios.cpm, ratios.cpc, ratios.cpa, ratios.cpe, ratios.cpl,
                ratios.roas, ratios.roi);
    }

    private AdvertiserMetrics toAdvertiserMetrics(Long advertiserId, int campaignCount, RawCounters raw) {
        Ratios ratios = computeRatios(raw);
        return new AdvertiserMetrics(advertiserId, campaignCount, raw.impressions, raw.clicks, raw.conversions,
                raw.clicks + raw.conversions, raw.conversionValue, raw.spend,
                ratios.ctr, ratios.cvr, ratios.vcr, ratios.cpm, ratios.cpc, ratios.cpa, ratios.cpe, ratios.cpl,
                ratios.roas, ratios.roi);
    }

    private Ratios computeRatios(RawCounters raw) {
        long engagement = raw.clicks + raw.conversions;
        double ctr = percentage(raw.clicks, raw.impressions);
        double cvr = percentage(raw.conversions, raw.clicks);
        double vcr = percentage(raw.videoCompletes, raw.videoStarts);
        BigDecimal cpm = perThousand(raw.spend, raw.impressions);
        BigDecimal cpc = perUnit(raw.spend, raw.clicks);
        BigDecimal cpa = perUnit(raw.spend, raw.conversions);
        BigDecimal cpe = perUnit(raw.spend, engagement);
        // CPL mirrors CPA: MeasurementEvent.conversionType is free text with no
        // structural lead/purchase distinction, so there's no separate "lead"
        // count to divide by yet.
        BigDecimal cpl = cpa;
        double roas = ratioOf(raw.conversionValue, raw.spend);
        double roi = raw.spend.compareTo(BigDecimal.ZERO) == 0 ? 0.0
                : raw.conversionValue.subtract(raw.spend)
                        .divide(raw.spend, SCALE, RoundingMode.HALF_UP).doubleValue() * 100.0;

        return new Ratios(ctr, cvr, vcr, cpm, cpc, cpa, cpe, cpl, roas, roi);
    }

    private static BigDecimal nullSafe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private static double percentage(long numerator, long denominator) {
        return denominator == 0 ? 0.0 : numerator * 100.0 / denominator;
    }

    private static BigDecimal perThousand(BigDecimal amount, long count) {
        return count == 0 ? BigDecimal.ZERO
                : amount.multiply(BigDecimal.valueOf(1000)).divide(BigDecimal.valueOf(count), SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal perUnit(BigDecimal amount, long count) {
        return count == 0 ? BigDecimal.ZERO : amount.divide(BigDecimal.valueOf(count), SCALE, RoundingMode.HALF_UP);
    }

    private static double ratioOf(BigDecimal numerator, BigDecimal denominator) {
        return denominator.compareTo(BigDecimal.ZERO) == 0 ? 0.0
                : numerator.divide(denominator, SCALE, RoundingMode.HALF_UP).doubleValue();
    }

    private record RawCounters(long impressions, long clicks, long conversions, BigDecimal conversionValue,
                                BigDecimal spend, long videoStarts, long videoCompletes) {
        static RawCounters zero() {
            return new RawCounters(0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO, 0, 0);
        }

        RawCounters plus(RawCounters other) {
            return new RawCounters(
                    impressions + other.impressions,
                    clicks + other.clicks,
                    conversions + other.conversions,
                    conversionValue.add(other.conversionValue),
                    spend.add(other.spend),
                    videoStarts + other.videoStarts,
                    videoCompletes + other.videoCompletes);
        }
    }

    private record Ratios(double ctr, double cvr, double vcr, BigDecimal cpm, BigDecimal cpc, BigDecimal cpa,
                           BigDecimal cpe, BigDecimal cpl, double roas, double roi) {
    }

    public record CampaignMetrics(Long campaignId, String campaignName, long impressions, long clicks,
                                   long conversions, long engagement, BigDecimal conversionValue, BigDecimal spend,
                                   double ctr, double cvr, double vcr, BigDecimal cpm, BigDecimal cpc,
                                   BigDecimal cpa, BigDecimal cpe, BigDecimal cpl, double roas, double roi) {
    }

    public record AdvertiserMetrics(Long advertiserId, int campaignCount, long impressions, long clicks,
                                     long conversions, long engagement, BigDecimal conversionValue, BigDecimal spend,
                                     double ctr, double cvr, double vcr, BigDecimal cpm, BigDecimal cpc,
                                     BigDecimal cpa, BigDecimal cpe, BigDecimal cpl, double roas, double roi) {
    }

    public record ChannelSyncSummary(Long connectorId, ConnectorType type, ConnectorStatus status, long totalSyncs,
                                      long successfulSyncs, long failedSyncs, LocalDateTime lastSyncedAt) {
    }
}
