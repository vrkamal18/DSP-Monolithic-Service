package com.adc.adcampaign.finance.pricing.repository;

import com.adc.adcampaign.finance.pricing.entity.TenantFeeConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantFeeConfigRepository extends JpaRepository<TenantFeeConfig, Long> {
    Optional<TenantFeeConfig> findByTenantId(Long tenantId);
}
