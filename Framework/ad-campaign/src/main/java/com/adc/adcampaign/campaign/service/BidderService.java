package com.adc.adcampaign.campaign.service;

import com.adc.adcampaign.campaign.dto.BidRequestDTO;
import com.adc.adcampaign.campaign.entity.*;
import com.adc.adcampaign.campaign.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class BidderService {

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private CreativeRepository creativeRepository;

    @Autowired
    private TargetingRuleRepository targetingRuleRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private AdImpressionService adImpressionService;

    public Optional<Creative> handleBidRequest(BidRequestDTO request) {
        List<Campaign> campaigns = campaignRepository.findAll();
    
        for (Campaign campaign : campaigns) {
            List<TargetingRule> rules = targetingRuleRepository.findByCampaignId(campaign.getId());
            if (!matchesTargeting(request, rules)) continue;
    
            Optional<Budget> budgetOpt = budgetRepository.findByCampaignId(campaign.getId());
            if (budgetOpt.isEmpty() || !hasBudget(budgetOpt.get())) continue;
    
            List<Creative> creatives = creativeRepository.findByCampaignId(campaign.getId());
            if (!creatives.isEmpty()) {
                Creative selectedCreative = creatives.get(0);
    
                // Log Impression + Update Budget
                AdImpression impression = AdImpression.builder()
                        .campaign(campaign)
                        .creative(selectedCreative)
                        .location(request.getLocation())
                        .ageGroup(request.getAgeGroup())
                        .deviceType(request.getDeviceType())
                        .build();
                adImpressionService.logImpression(impression);
    
                return Optional.of(selectedCreative);
            }
        }
        return Optional.empty();
    }
    

    private boolean matchesTargeting(BidRequestDTO request, List<TargetingRule> rules) {
        for (TargetingRule rule : rules) {
            if ("location".equalsIgnoreCase(rule.getRuleType()) &&
                !"include".equalsIgnoreCase(rule.getConditionType())) continue;
            if ("location".equalsIgnoreCase(rule.getRuleType()) &&
                !request.getLocation().equalsIgnoreCase(rule.getRuleValue())) return false;

            if ("age".equalsIgnoreCase(rule.getRuleType()) &&
                !"include".equalsIgnoreCase(rule.getConditionType())) continue;
            if ("age".equalsIgnoreCase(rule.getRuleType()) &&
                !request.getAgeGroup().equalsIgnoreCase(rule.getRuleValue())) return false;

            if ("device".equalsIgnoreCase(rule.getRuleType()) &&
                !"include".equalsIgnoreCase(rule.getConditionType())) continue;
            if ("device".equalsIgnoreCase(rule.getRuleType()) &&
                !request.getDeviceType().equalsIgnoreCase(rule.getRuleValue())) return false;
        }
        return true;
    }

    private boolean hasBudget(Budget budget) {
        return budget.getSpentAmount() == null ||
               budget.getSpentAmount() < budget.getTotalBudget();
    }
}

