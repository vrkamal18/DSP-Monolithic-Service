package com.adc.adcampaign.campaign.service;

import com.adc.adcampaign.campaign.entity.AdImpression;
import com.adc.adcampaign.campaign.entity.Budget;
import com.adc.adcampaign.campaign.repository.AdImpressionRepository;
import com.adc.adcampaign.campaign.repository.BudgetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

@Service
public class AdImpressionService {

    @Autowired
    private AdImpressionRepository adImpressionRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    private final Map<String, Double> dailySpendMap = new HashMap<>();

    public AdImpression logImpression(AdImpression impression) {
        // Save impression
        AdImpression saved = adImpressionRepository.save(impression);

        // Update budget
        Optional<Budget> budgetOpt = budgetRepository.findByCampaignId(impression.getCampaign().getId());
        if (budgetOpt.isPresent()) {
            Budget budget = budgetOpt.get();
            double newSpend = (budget.getSpentAmount() == null ? 0.0 : budget.getSpentAmount()) + 0.05; // Mock $0.05 cost
            budget.setSpentAmount(newSpend);
            budgetRepository.save(budget);

            // Update daily spend for pacing (in-memory map)
            String key = budget.getCampaign().getId() + ":" + LocalDate.now();
            double currentDaySpend = dailySpendMap.getOrDefault(key, 0.0);
            dailySpendMap.put(key, currentDaySpend + 0.05);
        }

        return saved;
    }

}


