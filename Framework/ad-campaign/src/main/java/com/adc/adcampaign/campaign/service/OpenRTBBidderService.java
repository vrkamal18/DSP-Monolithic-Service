package com.adc.adcampaign.campaign.service;

import com.adc.adcampaign.campaign.dto.openrtb.OpenRTBBidRequestDTO;
import com.adc.adcampaign.campaign.dto.openrtb.OpenRTBBidResponseDTO;
import com.adc.adcampaign.campaign.entity.*;
import com.adc.adcampaign.campaign.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class OpenRTBBidderService {

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

    private final Map<String, Integer> userFrequencyMap = new HashMap<>();
    private static final int MAX_IMPRESSIONS_PER_DAY = 5;

    private final Map<String, Double> dailySpendMap = new HashMap<>();

    public Optional<OpenRTBBidResponseDTO> processBid(OpenRTBBidRequestDTO request) {
        List<Campaign> allCampaigns = campaignRepository.findAll();
    
        // Find all eligible campaigns
        List<Campaign> eligible = new ArrayList<>();
        for (Campaign campaign : allCampaigns) {
            if (!matches(request, targetingRuleRepository.findByCampaignId(campaign.getId()))) continue;
    
            Optional<Budget> budgetOpt = budgetRepository.findByCampaignId(campaign.getId());
            if (budgetOpt.isEmpty() || !hasBudget(budgetOpt.get())) continue;
    
            List<Creative> creatives = creativeRepository.findByCampaignId(campaign.getId());
            if (creatives.isEmpty()) continue;
    
            eligible.add(campaign);
        }
    
        if (eligible.isEmpty()) return Optional.empty();
    
        // Pick the campaign with the highest bid price
        eligible.sort((c1, c2) -> Double.compare(
                Optional.ofNullable(c2.getBidPrice()).orElse(0.0),
                Optional.ofNullable(c1.getBidPrice()).orElse(0.0)
        ));
    
        Campaign winningCampaign = eligible.get(0);
        List<Creative> creatives = creativeRepository.findByCampaignId(winningCampaign.getId());
        Creative selectedCreative = creatives.get(0);
        
        String userId = request.getUser().getId();
        String dateKey = LocalDate.now().toString(); // e.g., 2025-03-30
        String key = winningCampaign.getId() + ":" + userId + ":" + dateKey;

        int count = userFrequencyMap.getOrDefault(key, 0);
        if (count >= MAX_IMPRESSIONS_PER_DAY) {
            return Optional.empty(); // frequency cap reached
        }

        // update frequency
        userFrequencyMap.put(key, count + 1);

        // Log Impression
        AdImpression impression = AdImpression.builder()
                .campaign(winningCampaign)
                .creative(selectedCreative)
                .location(request.getDevice().getGeo())
                .deviceType(request.getDevice().getDevicetype())
                .build();
        adImpressionService.logImpression(impression);
    
        // Build Response with bidPrice
        OpenRTBBidResponseDTO.SeatBid.Bid bid = OpenRTBBidResponseDTO.SeatBid.Bid.builder()
                .id(UUID.randomUUID().toString())
                .impid(request.getImp().getId())
                .price(winningCampaign.getBidPrice())
                .adm(selectedCreative.getUrl())
                .w(selectedCreative.getWidth())
                .h(selectedCreative.getHeight())
                .build();
    
        OpenRTBBidResponseDTO.SeatBid seatBid = OpenRTBBidResponseDTO.SeatBid.builder()
                .bid(List.of(bid))
                .build();
    
        return Optional.of(OpenRTBBidResponseDTO.builder()
                .id(request.getId())
                .seatbid(seatBid)
                .build());
    }

    private boolean matches(OpenRTBBidRequestDTO request, List<TargetingRule> rules) {
        for (TargetingRule rule : rules) {
            String ruleType = rule.getRuleType();
            String ruleVal = rule.getRuleValue();
            String condition = rule.getConditionType();

            if ("location".equalsIgnoreCase(ruleType) && "include".equalsIgnoreCase(condition)) {
                if (!Objects.equals(ruleVal, request.getDevice().getGeo())) return false;
            }

            if ("device".equalsIgnoreCase(ruleType) && "include".equalsIgnoreCase(condition)) {
                if (!Objects.equals(ruleVal, request.getDevice().getDevicetype())) return false;
            }
        }
        return true;
    }

    private boolean hasBudget(Budget budget) {
        double spent = Optional.ofNullable(budget.getSpentAmount()).orElse(0.0);
        double total = Optional.ofNullable(budget.getTotalBudget()).orElse(0.0);

        // Normal check
        if (spent >= total) return false;

        // Pacing logic
        if ("STANDARD".equalsIgnoreCase(budget.getPacingMode())) {
            LocalDate start = budget.getCampaign().getStartDate().toLocalDate();
            LocalDate end = budget.getCampaign().getEndDate().toLocalDate();
            long totalDays = ChronoUnit.DAYS.between(start, end) + 1;
            if (totalDays <= 0) return true;

            double dailyCap = total / totalDays;
            LocalDate today = LocalDate.now();

            // Check impressions today
            String key = budget.getCampaign().getId() + ":" + today;
            double spentToday = dailySpendMap.getOrDefault(key, 0.0);
            return spentToday < dailyCap;
        }

        // For ACCELERATED or default pacing: only total budget matters
        return true;
    }
}

