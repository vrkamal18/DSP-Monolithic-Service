package com.adc.adcampaign.campaign.repository;

import com.adc.adcampaign.campaign.entity.Creative;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CreativeRepository extends JpaRepository<Creative, Long> {
    List<Creative> findByCampaignId(Long campaignId);
}
