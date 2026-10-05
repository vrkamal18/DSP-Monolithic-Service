package com.adc.adcampaign.campaign.repository;

import com.adc.adcampaign.campaign.entity.Advertiser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdvertiserRepository extends JpaRepository<Advertiser, Long> {
    List<Advertiser> findByTenantId(Long tenantId);
    Optional<Advertiser> findByIdAndTenantId(Long id, Long tenantId);
}
