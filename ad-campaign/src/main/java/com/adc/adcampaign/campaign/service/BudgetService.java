package com.adc.adcampaign.campaign.service;

import com.adc.adcampaign.campaign.dto.BudgetRequest;
import com.adc.adcampaign.campaign.entity.Budget;
import com.adc.adcampaign.campaign.entity.Campaign;
import com.adc.adcampaign.campaign.repository.BudgetRepository;
import com.adc.adcampaign.campaign.repository.CampaignRepository;
import com.adc.adcampaign.foundation.error.NotFoundException;
import com.adc.adcampaign.foundation.security.TenantContext;
import com.adc.adcampaign.identity.entity.Tenant;
import com.adc.adcampaign.identity.repository.TenantRepository;
import org.springframework.stereotype.Service;

@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CampaignRepository campaignRepository;
    private final TenantRepository tenantRepository;

    public BudgetService(BudgetRepository budgetRepository,
                          CampaignRepository campaignRepository,
                          TenantRepository tenantRepository) {
        this.budgetRepository = budgetRepository;
        this.campaignRepository = campaignRepository;
        this.tenantRepository = tenantRepository;
    }

    public Budget createOrUpdateBudget(BudgetRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        Tenant tenant = tenantRepository.getReferenceById(tenantId);
        Campaign campaign = campaignRepository.findByIdAndTenantId(request.getCampaignId(), tenantId)
                .orElseThrow(() -> new NotFoundException("Campaign not found: " + request.getCampaignId()));

        Budget budget = budgetRepository.findByCampaignIdAndTenantId(request.getCampaignId(), tenantId)
                .orElseGet(() -> Budget.builder().tenant(tenant).campaign(campaign).build());

        budget.setTotalBudget(request.getTotalBudget());
        budget.setDailyCap(request.getDailyCap());
        budget.setSpentAmount(request.getSpentAmount());
        budget.setPacingMode(request.getPacingMode());
        return budgetRepository.save(budget);
    }

    public Budget getBudgetById(Long id) {
        return budgetRepository.findByIdAndTenantId(id, TenantContext.requireTenantId())
                .orElseThrow(() -> new NotFoundException("Budget not found: " + id));
    }

    public Budget getBudgetByCampaignId(Long campaignId) {
        return budgetRepository.findByCampaignIdAndTenantId(campaignId, TenantContext.requireTenantId())
                .orElseThrow(() -> new NotFoundException("Budget not found for campaign: " + campaignId));
    }

    public void deleteBudget(Long id) {
        budgetRepository.delete(getBudgetById(id));
    }
}
