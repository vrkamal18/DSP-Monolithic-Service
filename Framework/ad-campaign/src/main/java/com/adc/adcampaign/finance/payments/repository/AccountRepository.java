package com.adc.adcampaign.finance.payments.repository;

import com.adc.adcampaign.finance.payments.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByTenantIdAndAdvertiserId(Long tenantId, Long advertiserId);
}
