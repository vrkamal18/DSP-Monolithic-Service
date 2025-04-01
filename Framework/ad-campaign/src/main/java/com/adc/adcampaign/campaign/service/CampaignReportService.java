package com.adc.adcampaign.campaign.service;

import com.adc.adcampaign.campaign.dto.CampaignReportDTO;
import com.adc.adcampaign.campaign.entity.*;
import com.adc.adcampaign.campaign.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CampaignReportService {

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private AdImpressionRepository adImpressionRepository;

    @Autowired
    private AdClickRepository adClickRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    public CampaignReportDTO generateReport(Long campaignId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));

        long impressions = adImpressionRepository.countByCampaignId(campaignId);
        long clicks = adClickRepository.countByCampaignId(campaignId);
        double ctr = impressions > 0 ? (clicks * 100.0 / impressions) : 0.0;

        Budget budget = budgetRepository.findByCampaignId(campaignId)
                .orElseThrow(() -> new RuntimeException("Budget not found"));

        double spent = budget.getSpentAmount() != null ? budget.getSpentAmount() : 0.0;
        double total = budget.getTotalBudget() != null ? budget.getTotalBudget() : 0.0;
        double remaining = total - spent;

        return CampaignReportDTO.builder()
                .campaignId(campaignId)
                .campaignName(campaign.getName())
                .totalImpressions(impressions)
                .totalClicks(clicks)
                .ctr(ctr)
                .totalBudget(total)
                .spentAmount(spent)
                .remainingBudget(remaining)
                .build();
    }
}

