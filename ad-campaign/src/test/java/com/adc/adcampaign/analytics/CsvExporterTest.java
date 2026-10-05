package com.adc.adcampaign.analytics;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CsvExporterTest {

    private final CsvExporter csvExporter = new CsvExporter();

    private AnalyticsService.CampaignMetrics metrics(String campaignName) {
        return new AnalyticsService.CampaignMetrics(10L, campaignName, 1000, 100, 10, 110,
                BigDecimal.valueOf(500), BigDecimal.valueOf(100), 10.0, 10.0, 40.0,
                BigDecimal.valueOf(100), BigDecimal.valueOf(1), BigDecimal.valueOf(10),
                BigDecimal.valueOf(0.9), BigDecimal.valueOf(10), 5.0, 400.0);
    }

    @Test
    void producesHeaderAndOneDataRowWithExpectedValues() {
        String csv = csvExporter.campaignMetricsToCsv(metrics("Fall Sale"));
        String[] lines = csv.split("\n");

        assertThat(lines).hasSize(2);
        assertThat(lines[0]).isEqualTo(
                "campaignId,campaignName,impressions,clicks,conversions,engagement,conversionValue,spend,"
                        + "ctr,cvr,vcr,cpm,cpc,cpa,cpe,cpl,roas,roi");
        assertThat(lines[1]).startsWith("10,Fall Sale,1000,100,10,110,500,100,10.0,10.0,40.0");
    }

    @Test
    void quotesACampaignNameContainingAComma() {
        String csv = csvExporter.campaignMetricsToCsv(metrics("Fall Sale, Phase 2"));
        String[] lines = csv.split("\n");

        assertThat(lines[1]).startsWith("10,\"Fall Sale, Phase 2\",1000");
    }
}
