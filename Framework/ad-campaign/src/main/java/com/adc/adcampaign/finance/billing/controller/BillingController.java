package com.adc.adcampaign.finance.billing.controller;

import com.adc.adcampaign.finance.billing.BillingService;
import com.adc.adcampaign.finance.billing.dto.AdjustmentRequest;
import com.adc.adcampaign.finance.billing.dto.LedgerEntryResponse;
import com.adc.adcampaign.foundation.security.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/billing")
@Validated
public class BillingController {

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'FINANCE_USER', 'ANALYST')")
    @GetMapping("/entries")
    public ResponseEntity<List<LedgerEntryResponse>> getEntries() {
        return ResponseEntity.ok(billingService.getEntriesForTenant(TenantContext.requireTenantId()).stream()
                .map(LedgerEntryResponse::from)
                .toList());
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'FINANCE_USER', 'ANALYST')")
    @GetMapping("/entries/campaign/{campaignId}")
    public ResponseEntity<List<LedgerEntryResponse>> getEntriesByCampaign(@PathVariable Long campaignId) {
        return ResponseEntity.ok(billingService.getEntriesForCampaign(TenantContext.requireTenantId(), campaignId)
                .stream()
                .map(LedgerEntryResponse::from)
                .toList());
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'FINANCE_USER')")
    @PostMapping("/adjustments")
    public ResponseEntity<Void> createAdjustment(@Valid @RequestBody AdjustmentRequest request,
                                                  @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey) {
        billingService.recordAdjustment(TenantContext.requireTenantId(), request.getCampaignId(),
                request.getAmount(), idempotencyKey, TenantContext.requireUserId());
        return ResponseEntity.accepted().build();
    }
}
