package com.adc.adcampaign.campaign.service;

import com.adc.adcampaign.campaign.dto.TargetingRuleRequest;
import com.adc.adcampaign.campaign.entity.Campaign;
import com.adc.adcampaign.campaign.entity.TargetingRule;
import com.adc.adcampaign.campaign.repository.CampaignRepository;
import com.adc.adcampaign.campaign.repository.TargetingRuleRepository;
import com.adc.adcampaign.foundation.error.NotFoundException;
import com.adc.adcampaign.foundation.security.TenantContext;
import com.adc.adcampaign.identity.entity.Tenant;
import com.adc.adcampaign.identity.repository.TenantRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TargetingRuleService {

    private final TargetingRuleRepository targetingRuleRepository;
    private final CampaignRepository campaignRepository;
    private final TenantRepository tenantRepository;

    public TargetingRuleService(TargetingRuleRepository targetingRuleRepository,
                                 CampaignRepository campaignRepository,
                                 TenantRepository tenantRepository) {
        this.targetingRuleRepository = targetingRuleRepository;
        this.campaignRepository = campaignRepository;
        this.tenantRepository = tenantRepository;
    }

    public TargetingRule createTargetingRule(TargetingRuleRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        Tenant tenant = tenantRepository.getReferenceById(tenantId);
        Campaign campaign = resolveCampaign(request.getCampaignId(), tenantId);

        TargetingRule rule = TargetingRule.builder()
                .tenant(tenant)
                .campaign(campaign)
                .ruleType(request.getRuleType())
                .ruleValue(request.getRuleValue())
                .conditionType(request.getConditionType())
                .build();
        return targetingRuleRepository.save(rule);
    }

    public List<TargetingRule> getAllTargetingRules() {
        return targetingRuleRepository.findByTenantId(TenantContext.requireTenantId());
    }

    public TargetingRule getTargetingRuleById(Long id) {
        return targetingRuleRepository.findByIdAndTenantId(id, TenantContext.requireTenantId())
                .orElseThrow(() -> new NotFoundException("Targeting rule not found: " + id));
    }

    public List<TargetingRule> getRulesByCampaign(Long campaignId) {
        Long tenantId = TenantContext.requireTenantId();
        return targetingRuleRepository.findByCampaignId(campaignId).stream()
                .filter(rule -> rule.getTenant().getId().equals(tenantId))
                .toList();
    }

    public TargetingRule updateTargetingRule(Long id, TargetingRuleRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        TargetingRule existing = getTargetingRuleById(id);
        existing.setCampaign(resolveCampaign(request.getCampaignId(), tenantId));
        existing.setRuleType(request.getRuleType());
        existing.setRuleValue(request.getRuleValue());
        existing.setConditionType(request.getConditionType());
        return targetingRuleRepository.save(existing);
    }

    public void deleteTargetingRule(Long id) {
        targetingRuleRepository.delete(getTargetingRuleById(id));
    }

    private Campaign resolveCampaign(Long campaignId, Long tenantId) {
        return campaignRepository.findByIdAndTenantId(campaignId, tenantId)
                .orElseThrow(() -> new NotFoundException("Campaign not found: " + campaignId));
    }
}
