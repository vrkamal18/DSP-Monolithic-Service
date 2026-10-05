package com.adc.adcampaign.campaign.repository;

import com.adc.adcampaign.campaign.entity.AdClick;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdClickRepository extends JpaRepository<AdClick, Long> {
    long countByCampaignId(Long campaignId);
}
