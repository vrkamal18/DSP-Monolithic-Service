package com.adc.adcampaign.finance.pricing.controller;

import com.adc.adcampaign.finance.pricing.FeeCalculationService;
import com.adc.adcampaign.finance.pricing.dto.FeeConfigRequest;
import com.adc.adcampaign.finance.pricing.dto.FeeConfigResponse;
import com.adc.adcampaign.foundation.security.TenantContext;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/fees")
public class FeeController {

    private final FeeCalculationService feeCalculationService;

    public FeeController(FeeCalculationService feeCalculationService) {
        this.feeCalculationService = feeCalculationService;
    }

    @GetMapping
    public ResponseEntity<FeeConfigResponse> getFeeConfig() {
        var percent = feeCalculationService.getPlatformFeePercent(TenantContext.requireTenantId());
        return ResponseEntity.ok(FeeConfigResponse.builder().platformFeePercent(percent).build());
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'FINANCE_USER')")
    @PutMapping
    public ResponseEntity<FeeConfigResponse> updateFeeConfig(@Valid @RequestBody FeeConfigRequest request) {
        var config = feeCalculationService.upsertFeeConfig(TenantContext.requireTenantId(),
                request.getPlatformFeePercent());
        return ResponseEntity.ok(FeeConfigResponse.builder().platformFeePercent(config.getPlatformFeePercent()).build());
    }
}
