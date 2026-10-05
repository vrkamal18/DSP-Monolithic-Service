package com.adc.adcampaign.campaign.repository;

import com.adc.adcampaign.campaign.entity.Creative;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CreativeRepository extends JpaRepository<Creative, Long> {
    List<Creative> findByCampaignId(Long campaignId);
    List<Creative> findByTenantId(Long tenantId);
    Optional<Creative> findByIdAndTenantId(Long id, Long tenantId);
    List<Creative> findByCampaignIdAndTenantId(Long campaignId, Long tenantId);
}
