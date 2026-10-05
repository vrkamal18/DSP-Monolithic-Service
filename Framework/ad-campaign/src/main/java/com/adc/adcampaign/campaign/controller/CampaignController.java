package com.adc.adcampaign.campaign.controller;

import com.adc.adcampaign.campaign.dto.CampaignRequest;
import com.adc.adcampaign.campaign.dto.CampaignResponse;
import com.adc.adcampaign.campaign.service.CampaignService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/campaigns")
public class CampaignController {

    private final CampaignService campaignService;

    public CampaignController(CampaignService campaignService) {
        this.campaignService = campaignService;
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'CAMPAIGN_MANAGER')")
    @PostMapping
    public ResponseEntity<CampaignResponse> createCampaign(@Valid @RequestBody CampaignRequest request) {
        return ResponseEntity.ok(CampaignResponse.from(campaignService.createCampaign(request)));
    }

    @GetMapping
    public ResponseEntity<List<CampaignResponse>> getAllCampaigns() {
        return ResponseEntity.ok(campaignService.getAllCampaigns().stream()
                .map(CampaignResponse::from)
                .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CampaignResponse> getCampaignById(@PathVariable Long id) {
        return ResponseEntity.ok(CampaignResponse.from(campaignService.getCampaignById(id)));
    }

    @GetMapping("/advertiser/{advertiserId}")
    public ResponseEntity<List<CampaignResponse>> getCampaignsByAdvertiser(@PathVariable Long advertiserId) {
        return ResponseEntity.ok(campaignService.getCampaignsByAdvertiser(advertiserId).stream()
                .map(CampaignResponse::from)
                .toList());
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'CAMPAIGN_MANAGER')")
    @PutMapping("/{id}")
    public ResponseEntity<CampaignResponse> updateCampaign(@PathVariable Long id,
                                                            @Valid @RequestBody CampaignRequest request) {
        return ResponseEntity.ok(CampaignResponse.from(campaignService.updateCampaign(id, request)));
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'CAMPAIGN_MANAGER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCampaign(@PathVariable Long id) {
        campaignService.deleteCampaign(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'CAMPAIGN_MANAGER')")
    @PostMapping("/{id}/activate")
    public ResponseEntity<CampaignResponse> activate(@PathVariable Long id) {
        return ResponseEntity.ok(CampaignResponse.from(campaignService.activate(id)));
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'CAMPAIGN_MANAGER')")
    @PostMapping("/{id}/pause")
    public ResponseEntity<CampaignResponse> pause(@PathVariable Long id) {
        return ResponseEntity.ok(CampaignResponse.from(campaignService.pause(id)));
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'CAMPAIGN_MANAGER')")
    @PostMapping("/{id}/resume")
    public ResponseEntity<CampaignResponse> resume(@PathVariable Long id) {
        return ResponseEntity.ok(CampaignResponse.from(campaignService.resume(id)));
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'CAMPAIGN_MANAGER')")
    @PostMapping("/{id}/complete")
    public ResponseEntity<CampaignResponse> complete(@PathVariable Long id) {
        return ResponseEntity.ok(CampaignResponse.from(campaignService.complete(id)));
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'CAMPAIGN_MANAGER')")
    @PostMapping("/{id}/archive")
    public ResponseEntity<CampaignResponse> archive(@PathVariable Long id) {
        return ResponseEntity.ok(CampaignResponse.from(campaignService.archive(id)));
    }
}
