package com.adc.adcampaign.campaign.controller;

import com.adc.adcampaign.campaign.entity.AdClick;
import com.adc.adcampaign.campaign.service.AdClickService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clicks")
public class ClickController {

    @Autowired
    private AdClickService adClickService;

    @PostMapping
    public ResponseEntity<AdClick> logClick(@RequestBody AdClick click) {
        return ResponseEntity.ok(adClickService.logClick(click));
    }
}

