package com.adc.adcampaign.measurement.controller;

import com.adc.adcampaign.foundation.security.TenantContext;
import com.adc.adcampaign.measurement.MeasurementService;
import com.adc.adcampaign.measurement.dto.MeasurementSummaryResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/measurement")
public class MeasurementController {

    private final MeasurementService measurementService;

    public MeasurementController(MeasurementService measurementService) {
        this.measurementService = measurementService;
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'CAMPAIGN_MANAGER', 'ANALYST')")
    @GetMapping("/campaigns/{campaignId}/summary")
    public ResponseEntity<MeasurementSummaryResponse> summary(@PathVariable Long campaignId) {
        var summary = measurementService.getSummary(TenantContext.requireTenantId(), campaignId);
        return ResponseEntity.ok(MeasurementSummaryResponse.from(campaignId, summary));
    }
}
