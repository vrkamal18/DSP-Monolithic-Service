package com.adc.adcampaign.campaign.repository;

import com.adc.adcampaign.campaign.entity.CampaignStatusChange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CampaignStatusChangeRepository extends JpaRepository<CampaignStatusChange, Long> {
}
