package com.adc.adcampaign.campaign.repository;

import com.adc.adcampaign.campaign.entity.AdImpression;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdImpressionRepository extends JpaRepository<AdImpression, Long> {

    long countByCampaignId(Long campaignId);

}

