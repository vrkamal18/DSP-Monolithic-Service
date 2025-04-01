package com.adc.adcampaign.campaign.repository;

import com.adc.adcampaign.campaign.entity.TargetingRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TargetingRuleRepository extends JpaRepository<TargetingRule, Long> {
    List<TargetingRule> findByCampaignId(Long campaignId);
}

