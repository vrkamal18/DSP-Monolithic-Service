package com.adc.adcampaign.campaign.service;

import com.adc.adcampaign.campaign.entity.TargetingRule;
import com.adc.adcampaign.campaign.repository.TargetingRuleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TargetingRuleService {

    @Autowired
    private TargetingRuleRepository targetingRuleRepository;

    public TargetingRule createTargetingRule(TargetingRule rule) {
        return targetingRuleRepository.save(rule);
    }

    public List<TargetingRule> getAllTargetingRules() {
        return targetingRuleRepository.findAll();
    }

    public Optional<TargetingRule> getTargetingRuleById(Long id) {
        return targetingRuleRepository.findById(id);
    }

    public List<TargetingRule> getRulesByCampaign(Long campaignId) {
        return targetingRuleRepository.findByCampaignId(campaignId);
    }

    public TargetingRule updateTargetingRule(TargetingRule rule) {
        return targetingRuleRepository.save(rule);
    }

    public void deleteTargetingRule(Long id) {
        targetingRuleRepository.deleteById(id);
    }
}


