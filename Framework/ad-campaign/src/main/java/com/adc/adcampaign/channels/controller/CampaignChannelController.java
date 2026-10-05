package com.adc.adcampaign.channels.controller;

import com.adc.adcampaign.channels.CampaignChannelService;
import com.adc.adcampaign.channels.dto.AssignCampaignChannelRequest;
import com.adc.adcampaign.channels.dto.CampaignChannelResponse;
import com.adc.adcampaign.channels.entity.ChannelType;
import com.adc.adcampaign.foundation.security.TenantContext;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Lives in the channels package, not campaign — this association is owned
 * by Channels per service/documents/01-service-architecture.md §6
 * ("channels -> campaign contracts"), so it's a new controller here rather
 * than a change to the existing CampaignController.
 */
@RestController
@RequestMapping("/api/v1/campaigns/{campaignId}/channels")
public class CampaignChannelController {

    private final CampaignChannelService campaignChannelService;

    public CampaignChannelController(CampaignChannelService campaignChannelService) {
        this.campaignChannelService = campaignChannelService;
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'CAMPAIGN_MANAGER')")
    @PostMapping
    public ResponseEntity<CampaignChannelResponse> assign(@PathVariable Long campaignId,
                                                           @Valid @RequestBody AssignCampaignChannelRequest request) {
        var association = campaignChannelService.assign(TenantContext.requireTenantId(), campaignId,
                request.getChannelType());
        return ResponseEntity.ok(CampaignChannelResponse.from(association));
    }

    @GetMapping
    public ResponseEntity<List<CampaignChannelResponse>> list(@PathVariable Long campaignId) {
        return ResponseEntity.ok(campaignChannelService.listForCampaign(TenantContext.requireTenantId(), campaignId)
                .stream()
                .map(CampaignChannelResponse::from)
                .toList());
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'CAMPAIGN_MANAGER')")
    @DeleteMapping("/{channelType}")
    public ResponseEntity<Void> remove(@PathVariable Long campaignId, @PathVariable ChannelType channelType) {
        campaignChannelService.remove(TenantContext.requireTenantId(), campaignId, channelType);
        return ResponseEntity.noContent().build();
    }
}
