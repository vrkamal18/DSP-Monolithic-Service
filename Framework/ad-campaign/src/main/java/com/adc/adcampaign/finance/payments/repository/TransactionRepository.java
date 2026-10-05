package com.adc.adcampaign.finance.payments.repository;

import com.adc.adcampaign.finance.payments.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByTenantIdAndAdvertiserId(Long tenantId, Long advertiserId);
    Optional<Transaction> findByTenantIdAndSourceEventId(Long tenantId, String sourceEventId);
}
