package com.adc.adcampaign.campaign.service;

import com.adc.adcampaign.campaign.entity.Budget;
import com.adc.adcampaign.campaign.repository.BudgetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class BudgetService {

    @Autowired
    private BudgetRepository budgetRepository;

    public Budget createOrUpdateBudget(Budget budget) {
        return budgetRepository.save(budget);
    }

    public Optional<Budget> getBudgetById(Long id) {
        return budgetRepository.findById(id);
    }

    public Optional<Budget> getBudgetByCampaignId(Long campaignId) {
        return budgetRepository.findByCampaignId(campaignId);
    }

    public void deleteBudget(Long id) {
        budgetRepository.deleteById(id);
    }
}

