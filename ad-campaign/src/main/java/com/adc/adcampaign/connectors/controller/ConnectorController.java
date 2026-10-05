package com.adc.adcampaign.connectors.controller;

import com.adc.adcampaign.connectors.ConnectorService;
import com.adc.adcampaign.connectors.dto.ConnectRequest;
import com.adc.adcampaign.connectors.dto.ConnectorActivityResponse;
import com.adc.adcampaign.connectors.dto.ConnectorResponse;
import com.adc.adcampaign.connectors.dto.ExternalIdMappingResponse;
import com.adc.adcampaign.foundation.security.TenantContext;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/connectors")
public class ConnectorController {

    private final ConnectorService connectorService;

    public ConnectorController(ConnectorService connectorService) {
        this.connectorService = connectorService;
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'CAMPAIGN_MANAGER')")
    @PostMapping
    public ResponseEntity<ConnectorResponse> connect(@Valid @RequestBody ConnectRequest request) {
        var connector = connectorService.connect(TenantContext.requireTenantId(), request.getType(),
                request.getExternalAccountId(), request.getCredential());
        return ResponseEntity.ok(ConnectorResponse.from(connector));
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'CAMPAIGN_MANAGER', 'ANALYST')")
    @GetMapping
    public ResponseEntity<List<ConnectorResponse>> list() {
        return ResponseEntity.ok(connectorService.listForTenant(TenantContext.requireTenantId()).stream()
                .map(ConnectorResponse::from)
                .toList());
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'CAMPAIGN_MANAGER', 'ANALYST')")
    @GetMapping("/{id}")
    public ResponseEntity<ConnectorResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ConnectorResponse.from(connectorService.getById(TenantContext.requireTenantId(), id)));
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'CAMPAIGN_MANAGER', 'ANALYST')")
    @GetMapping("/{id}/activity")
    public ResponseEntity<List<ConnectorActivityResponse>> activity(@PathVariable Long id) {
        return ResponseEntity.ok(connectorService.listActivity(TenantContext.requireTenantId(), id).stream()
                .map(ConnectorActivityResponse::from)
                .toList());
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'CAMPAIGN_MANAGER')")
    @PostMapping("/{id}/disconnect")
    public ResponseEntity<ConnectorResponse> disconnect(@PathVariable Long id) {
        return ResponseEntity.ok(ConnectorResponse.from(connectorService.disconnect(TenantContext.requireTenantId(), id)));
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'CAMPAIGN_MANAGER')")
    @PostMapping("/{id}/test")
    public ResponseEntity<ConnectorResponse> test(@PathVariable Long id) {
        return ResponseEntity.ok(ConnectorResponse.from(connectorService.test(TenantContext.requireTenantId(), id)));
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'CAMPAIGN_MANAGER')")
    @PostMapping("/{id}/sync")
    public ResponseEntity<ConnectorResponse> sync(@PathVariable Long id) {
        return ResponseEntity.ok(ConnectorResponse.from(connectorService.sync(TenantContext.requireTenantId(), id)));
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'CAMPAIGN_MANAGER')")
    @PostMapping("/{id}/campaigns/{campaignId}/push")
    public ResponseEntity<ExternalIdMappingResponse> push(@PathVariable Long id, @PathVariable Long campaignId) {
        var mapping = connectorService.pushCampaign(TenantContext.requireTenantId(), id, campaignId);
        return ResponseEntity.ok(ExternalIdMappingResponse.from(mapping));
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'CAMPAIGN_MANAGER')")
    @PostMapping("/{id}/campaigns/{campaignId}/pause")
    public ResponseEntity<Void> pause(@PathVariable Long id, @PathVariable Long campaignId) {
        connectorService.pauseCampaign(TenantContext.requireTenantId(), id, campaignId);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'CAMPAIGN_MANAGER')")
    @PostMapping("/{id}/campaigns/{campaignId}/resume")
    public ResponseEntity<Void> resume(@PathVariable Long id, @PathVariable Long campaignId) {
        connectorService.resumeCampaign(TenantContext.requireTenantId(), id, campaignId);
        return ResponseEntity.ok().build();
    }
}
