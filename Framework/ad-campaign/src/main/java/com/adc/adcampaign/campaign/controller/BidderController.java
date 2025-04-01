package com.adc.adcampaign.campaign.controller;

import com.adc.adcampaign.campaign.dto.BidRequestDTO;
import com.adc.adcampaign.campaign.service.BidderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bid")
public class BidderController {

    @Autowired
    private BidderService bidderService;

    @PostMapping
    public ResponseEntity<?> handleBid(@RequestBody BidRequestDTO bidRequest) {
        return bidderService.handleBidRequest(bidRequest)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build()); // 204 No match found
    }
}

