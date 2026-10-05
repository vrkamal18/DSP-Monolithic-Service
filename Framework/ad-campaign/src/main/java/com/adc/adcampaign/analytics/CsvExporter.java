package com.adc.adcampaign.analytics;

import org.springframework.stereotype.Component;

/**
 * Hand-rolled CSV export — the first in this codebase. A single-row export
 * doesn't justify adding opencsv/commons-csv to the classpath.
 */
@Component
public class CsvExporter {

    private static final String[] HEADERS = {
            "campaignId", "campaignName", "impressions", "clicks", "conversions", "engagement",
            "conversionValue", "spend", "ctr", "cvr", "vcr", "cpm", "cpc", "cpa", "cpe", "cpl", "roas", "roi"
    };

    public String campaignMetricsToCsv(AnalyticsService.CampaignMetrics metrics) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", HEADERS)).append("\n");
        sb.append(metrics.campaignId()).append(',')
                .append(escape(metrics.campaignName())).append(',')
                .append(metrics.impressions()).append(',')
                .append(metrics.clicks()).append(',')
                .append(metrics.conversions()).append(',')
                .append(metrics.engagement()).append(',')
                .append(metrics.conversionValue()).append(',')
                .append(metrics.spend()).append(',')
                .append(metrics.ctr()).append(',')
                .append(metrics.cvr()).append(',')
                .append(metrics.vcr()).append(',')
                .append(metrics.cpm()).append(',')
                .append(metrics.cpc()).append(',')
                .append(metrics.cpa()).append(',')
                .append(metrics.cpe()).append(',')
                .append(metrics.cpl()).append(',')
                .append(metrics.roas()).append(',')
                .append(metrics.roi()).append('\n');
        return sb.toString();
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
