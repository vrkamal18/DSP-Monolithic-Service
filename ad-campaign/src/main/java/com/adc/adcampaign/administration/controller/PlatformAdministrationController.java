package com.adc.adcampaign.administration.controller;

import com.adc.adcampaign.administration.PlatformAdministrationService;
import com.adc.adcampaign.administration.dto.PlatformHealthResponse;
import com.adc.adcampaign.administration.dto.TenantResponse;
import com.adc.adcampaign.foundation.security.TenantContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class PlatformAdministrationController {

    private final PlatformAdministrationService platformAdministrationService;

    public PlatformAdministrationController(PlatformAdministrationService platformAdministrationService) {
        this.platformAdministrationService = platformAdministrationService;
    }

    @GetMapping("/tenants")
    public ResponseEntity<List<TenantResponse>> listTenants() {
        return ResponseEntity.ok(platformAdministrationService.listTenants().stream()
                .map(TenantResponse::from)
                .toList());
    }

    @PostMapping("/tenants/{tenantId}/deactivate")
    public ResponseEntity<Void> deactivateTenant(@PathVariable Long tenantId) {
        platformAdministrationService.deactivateTenant(TenantContext.requireUserId(),
                TenantContext.requireTenantId(), tenantId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/tenants/{tenantId}/reactivate")
    public ResponseEntity<Void> reactivateTenant(@PathVariable Long tenantId) {
        platformAdministrationService.reactivateTenant(TenantContext.requireUserId(),
                TenantContext.requireTenantId(), tenantId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/platform/health")
    public ResponseEntity<PlatformHealthResponse> platformHealth() {
        return ResponseEntity.ok(PlatformHealthResponse.from(platformAdministrationService.getPlatformHealth()));
    }
}
