package com.adc.adcampaign.finance.pricing;

import com.adc.adcampaign.finance.pricing.entity.TenantFeeConfig;
import com.adc.adcampaign.finance.pricing.repository.TenantFeeConfigRepository;
import com.adc.adcampaign.identity.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class FeeCalculationServiceTest {

    @Mock private TenantFeeConfigRepository tenantFeeConfigRepository;
    @Mock private TenantRepository tenantRepository;

    private FeeCalculationService feeCalculationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        feeCalculationService = new FeeCalculationService(tenantFeeConfigRepository, tenantRepository);
    }

    @Test
    void calculatesFeeUsingTheConfiguredPercent() {
        when(tenantFeeConfigRepository.findByTenantId(1L)).thenReturn(Optional.of(
                TenantFeeConfig.builder().platformFeePercent(BigDecimal.valueOf(0.10)).build()));

        BigDecimal fee = feeCalculationService.calculatePlatformFee(BigDecimal.valueOf(2.00), 1L);

        assertThat(fee).isEqualByComparingTo("0.2000");
    }

    @Test
    void unconfiguredTenantIsChargedZeroFee() {
        when(tenantFeeConfigRepository.findByTenantId(1L)).thenReturn(Optional.empty());

        BigDecimal fee = feeCalculationService.calculatePlatformFee(BigDecimal.valueOf(2.00), 1L);

        assertThat(fee).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
