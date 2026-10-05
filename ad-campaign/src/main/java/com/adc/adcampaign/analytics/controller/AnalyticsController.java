package com.adc.adcampaign.analytics.controller;

import com.adc.adcampaign.analytics.AnalyticsService;
import com.adc.adcampaign.analytics.CsvExporter;
import com.adc.adcampaign.analytics.dto.AdvertiserAnalyticsResponse;
import com.adc.adcampaign.analytics.dto.CampaignAnalyticsResponse;
import com.adc.adcampaign.analytics.dto.ChannelSummaryResponse;
import com.adc.adcampaign.foundation.security.TenantContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@PreAuthorize("hasAnyRole('TENANT_ADMIN', 'CAMPAIGN_MANAGER', 'ANALYST')")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final CsvExporter csvExporter;

    public AnalyticsController(AnalyticsService analyticsService, CsvExporter csvExporter) {
        this.analyticsService = analyticsService;
        this.csvExporter = csvExporter;
    }

    @GetMapping("/campaigns/{campaignId}")
    public ResponseEntity<CampaignAnalyticsResponse> campaignMetrics(@PathVariable Long campaignId) {
        var metrics = analyticsService.getCampaignMetrics(TenantContext.requireTenantId(), campaignId);
        return ResponseEntity.ok(CampaignAnalyticsResponse.from(metrics));
    }

    @GetMapping("/campaigns/{campaignId}/export.csv")
    public ResponseEntity<String> campaignMetricsCsv(@PathVariable Long campaignId) {
        var metrics = analyticsService.getCampaignMetrics(TenantContext.requireTenantId(), campaignId);
        String csv = csvExporter.campaignMetricsToCsv(metrics);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"campaign-" + campaignId + "-report.csv\"")
                .body(csv);
    }

    @GetMapping("/advertisers/{advertiserId}")
    public ResponseEntity<AdvertiserAnalyticsResponse> advertiserMetrics(@PathVariable Long advertiserId) {
        var metrics = analyticsService.getAdvertiserMetrics(TenantContext.requireTenantId(), advertiserId);
        return ResponseEntity.ok(AdvertiserAnalyticsResponse.from(metrics));
    }

    @GetMapping("/channels/summary")
    public ResponseEntity<List<ChannelSummaryResponse>> channelSummary() {
        return ResponseEntity.ok(analyticsService.getCrossChannelSummary(TenantContext.requireTenantId()).stream()
                .map(ChannelSummaryResponse::from)
                .toList());
    }
}
