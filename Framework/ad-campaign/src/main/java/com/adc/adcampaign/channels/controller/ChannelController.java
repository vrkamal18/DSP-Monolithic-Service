package com.adc.adcampaign.channels.controller;

import com.adc.adcampaign.campaign.repository.CreativeRepository;
import com.adc.adcampaign.channels.ChannelMarkupService;
import com.adc.adcampaign.channels.ChannelService;
import com.adc.adcampaign.channels.dto.ChannelResponse;
import com.adc.adcampaign.channels.dto.ChannelToggleRequest;
import com.adc.adcampaign.channels.dto.MarkupPreviewResponse;
import com.adc.adcampaign.channels.entity.ChannelType;
import com.adc.adcampaign.foundation.error.NotFoundException;
import com.adc.adcampaign.foundation.security.TenantContext;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/channels")
public class ChannelController {

    private final ChannelService channelService;
    private final ChannelMarkupService channelMarkupService;
    private final CreativeRepository creativeRepository;

    public ChannelController(ChannelService channelService,
                              ChannelMarkupService channelMarkupService,
                              CreativeRepository creativeRepository) {
        this.channelService = channelService;
        this.channelMarkupService = channelMarkupService;
        this.creativeRepository = creativeRepository;
    }

    @GetMapping
    public ResponseEntity<List<ChannelResponse>> list() {
        return ResponseEntity.ok(channelService.listForTenant(TenantContext.requireTenantId()).stream()
                .map(ChannelResponse::from)
                .toList());
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'CAMPAIGN_MANAGER')")
    @PutMapping("/{type}")
    public ResponseEntity<ChannelResponse> setEnabled(@PathVariable ChannelType type,
                                                       @Valid @RequestBody ChannelToggleRequest request) {
        var channel = channelService.setEnabled(TenantContext.requireTenantId(), type, request.getEnabled());
        return ResponseEntity.ok(ChannelResponse.from(channel));
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'CAMPAIGN_MANAGER', 'CREATIVE_REVIEWER')")
    @GetMapping("/markup-preview/{creativeId}")
    public ResponseEntity<MarkupPreviewResponse> previewMarkup(@PathVariable Long creativeId) {
        var creative = creativeRepository.findByIdAndTenantId(creativeId, TenantContext.requireTenantId())
                .orElseThrow(() -> new NotFoundException("Creative not found: " + creativeId));
        String markup = channelMarkupService.buildMarkup(creative);
        return ResponseEntity.ok(MarkupPreviewResponse.builder().creativeId(creativeId).markup(markup).build());
    }
}
