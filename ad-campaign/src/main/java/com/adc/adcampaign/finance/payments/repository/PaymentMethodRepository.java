package com.adc.adcampaign.finance.payments.repository;

import com.adc.adcampaign.finance.payments.entity.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {
    List<PaymentMethod> findByTenantIdAndAdvertiserId(Long tenantId, Long advertiserId);
    Optional<PaymentMethod> findByIdAndTenantIdAndAdvertiserId(Long id, Long tenantId, Long advertiserId);
    Optional<PaymentMethod> findByIdAndTenantId(Long id, Long tenantId);
}
