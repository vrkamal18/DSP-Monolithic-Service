package com.adc.adcampaign.campaign.repository;

import com.adc.adcampaign.campaign.entity.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {
    List<Campaign> findByAdvertiserId(Long advertiserId);
}
