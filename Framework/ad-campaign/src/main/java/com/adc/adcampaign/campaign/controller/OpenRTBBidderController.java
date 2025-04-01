package com.adc.adcampaign.campaign.controller;

import com.adc.adcampaign.campaign.dto.openrtb.OpenRTBBidRequestDTO;
import com.adc.adcampaign.campaign.dto.openrtb.OpenRTBBidResponseDTO;
import com.adc.adcampaign.campaign.service.OpenRTBBidderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/openrtb2")
public class OpenRTBBidderController {

    @Autowired
    private OpenRTBBidderService openRTBBidderService;

    @PostMapping("/bidrequest")
    public ResponseEntity<OpenRTBBidResponseDTO> handleBid(@RequestBody OpenRTBBidRequestDTO request) {
        return openRTBBidderService.processBid(request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build()); // 204 if no matching campaign
    }
}

