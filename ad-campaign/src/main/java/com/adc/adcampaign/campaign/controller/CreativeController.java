package com.adc.adcampaign.campaign.controller;

import com.adc.adcampaign.campaign.dto.CreativeRequest;
import com.adc.adcampaign.campaign.dto.CreativeResponse;
import com.adc.adcampaign.campaign.dto.RejectCreativeRequest;
import com.adc.adcampaign.campaign.service.CreativeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/creatives")
public class CreativeController {

    private final CreativeService creativeService;

    public CreativeController(CreativeService creativeService) {
        this.creativeService = creativeService;
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'CAMPAIGN_MANAGER')")
    @PostMapping
    public ResponseEntity<CreativeResponse> createCreative(@Valid @RequestBody CreativeRequest request) {
        return ResponseEntity.ok(CreativeResponse.from(creativeService.createCreative(request)));
    }

    @GetMapping
    public ResponseEntity<List<CreativeResponse>> getAllCreatives() {
        return ResponseEntity.ok(creativeService.getAllCreatives().stream()
                .map(CreativeResponse::from)
                .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CreativeResponse> getCreativeById(@PathVariable Long id) {
        return ResponseEntity.ok(CreativeResponse.from(creativeService.getCreativeById(id)));
    }

    @GetMapping("/campaign/{campaignId}")
    public ResponseEntity<List<CreativeResponse>> getCreativesByCampaign(@PathVariable Long campaignId) {
        return ResponseEntity.ok(creativeService.getCreativesByCampaign(campaignId).stream()
                .map(CreativeResponse::from)
                .toList());
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'CAMPAIGN_MANAGER', 'CREATIVE_REVIEWER')")
    @PutMapping("/{id}")
    public ResponseEntity<CreativeResponse> updateCreative(@PathVariable Long id,
                                                            @Valid @RequestBody CreativeRequest request) {
        return ResponseEntity.ok(CreativeResponse.from(creativeService.updateCreative(id, request)));
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'CAMPAIGN_MANAGER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCreative(@PathVariable Long id) {
        creativeService.deleteCreative(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'CAMPAIGN_MANAGER')")
    @PostMapping("/{id}/submit")
    public ResponseEntity<CreativeResponse> submit(@PathVariable Long id) {
        return ResponseEntity.ok(CreativeResponse.from(creativeService.submit(id)));
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'CREATIVE_REVIEWER')")
    @PostMapping("/{id}/approve")
    public ResponseEntity<CreativeResponse> approve(@PathVariable Long id) {
        return ResponseEntity.ok(CreativeResponse.from(creativeService.approve(id)));
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'CREATIVE_REVIEWER')")
    @PostMapping("/{id}/reject")
    public ResponseEntity<CreativeResponse> reject(@PathVariable Long id,
                                                    @Valid @RequestBody RejectCreativeRequest request) {
        return ResponseEntity.ok(CreativeResponse.from(creativeService.reject(id, request.getReason())));
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'CAMPAIGN_MANAGER')")
    @PostMapping("/{id}/archive")
    public ResponseEntity<CreativeResponse> archive(@PathVariable Long id) {
        return ResponseEntity.ok(CreativeResponse.from(creativeService.archive(id)));
    }
}
