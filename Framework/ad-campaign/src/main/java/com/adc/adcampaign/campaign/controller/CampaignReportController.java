package com.adc.adcampaign.campaign.controller;

import com.adc.adcampaign.campaign.dto.CampaignReportDTO;
import com.adc.adcampaign.campaign.service.CampaignReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
public class CampaignReportController {

    @Autowired
    private CampaignReportService campaignReportService;

    @GetMapping("/campaign/{campaignId}")
    public ResponseEntity<CampaignReportDTO> getCampaignReport(@PathVariable Long campaignId) {
        CampaignReportDTO report = campaignReportService.generateReport(campaignId);
        return ResponseEntity.ok(report);
    }
}

