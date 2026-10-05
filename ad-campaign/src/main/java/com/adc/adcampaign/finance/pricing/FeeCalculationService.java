package com.adc.adcampaign.finance.pricing;

import com.adc.adcampaign.finance.pricing.entity.TenantFeeConfig;
import com.adc.adcampaign.finance.pricing.repository.TenantFeeConfigRepository;
import com.adc.adcampaign.identity.repository.TenantRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Deterministic platform-fee calculation for billing, per
 * service/documents/02-module-specifications.md §13.1.
 */
@Service
public class FeeCalculationService {

    private final TenantFeeConfigRepository tenantFeeConfigRepository;
    private final TenantRepository tenantRepository;

    public FeeCalculationService(TenantFeeConfigRepository tenantFeeConfigRepository,
                                  TenantRepository tenantRepository) {
        this.tenantFeeConfigRepository = tenantFeeConfigRepository;
        this.tenantRepository = tenantRepository;
    }

    public BigDecimal getPlatformFeePercent(Long tenantId) {
        return tenantFeeConfigRepository.findByTenantId(tenantId)
                .map(TenantFeeConfig::getPlatformFeePercent)
                .orElse(BigDecimal.ZERO);
    }

    public BigDecimal calculatePlatformFee(BigDecimal mediaCost, Long tenantId) {
        BigDecimal percent = getPlatformFeePercent(tenantId);
        return mediaCost.multiply(percent).setScale(4, RoundingMode.HALF_UP);
    }

    public TenantFeeConfig upsertFeeConfig(Long tenantId, BigDecimal platformFeePercent) {
        TenantFeeConfig config = tenantFeeConfigRepository.findByTenantId(tenantId)
                .orElseGet(() -> TenantFeeConfig.builder()
                        .tenant(tenantRepository.getReferenceById(tenantId))
                        .build());
        config.setPlatformFeePercent(platformFeePercent);
        return tenantFeeConfigRepository.save(config);
    }
}
