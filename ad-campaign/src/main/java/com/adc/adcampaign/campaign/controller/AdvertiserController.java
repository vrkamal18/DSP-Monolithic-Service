package com.adc.adcampaign.campaign.controller;

import com.adc.adcampaign.campaign.dto.AdvertiserRequest;
import com.adc.adcampaign.campaign.dto.AdvertiserResponse;
import com.adc.adcampaign.campaign.service.AdvertiserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/advertisers")
public class AdvertiserController {

    private final AdvertiserService advertiserService;

    public AdvertiserController(AdvertiserService advertiserService) {
        this.advertiserService = advertiserService;
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'CAMPAIGN_MANAGER')")
    @PostMapping
    public ResponseEntity<AdvertiserResponse> createAdvertiser(@Valid @RequestBody AdvertiserRequest request) {
        return ResponseEntity.ok(AdvertiserResponse.from(advertiserService.createAdvertiser(request)));
    }

    @GetMapping
    public ResponseEntity<List<AdvertiserResponse>> getAllAdvertisers() {
        return ResponseEntity.ok(advertiserService.getAllAdvertisers().stream()
                .map(AdvertiserResponse::from)
                .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdvertiserResponse> getAdvertiserById(@PathVariable Long id) {
        return ResponseEntity.ok(AdvertiserResponse.from(advertiserService.getAdvertiserById(id)));
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'CAMPAIGN_MANAGER')")
    @PutMapping("/{id}")
    public ResponseEntity<AdvertiserResponse> updateAdvertiser(@PathVariable Long id,
                                                                @Valid @RequestBody AdvertiserRequest request) {
        return ResponseEntity.ok(AdvertiserResponse.from(advertiserService.updateAdvertiser(id, request)));
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'CAMPAIGN_MANAGER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAdvertiser(@PathVariable Long id) {
        advertiserService.deleteAdvertiser(id);
        return ResponseEntity.noContent().build();
    }
}
