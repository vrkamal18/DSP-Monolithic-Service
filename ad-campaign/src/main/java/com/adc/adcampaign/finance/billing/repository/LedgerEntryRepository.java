package com.adc.adcampaign.finance.billing.repository;

import com.adc.adcampaign.finance.billing.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
    List<LedgerEntry> findByTenantId(Long tenantId);
    List<LedgerEntry> findByTenantIdAndCampaignId(Long tenantId, Long campaignId);
    Optional<LedgerEntry> findByTenantIdAndSourceEventId(Long tenantId, String sourceEventId);

    /** Used by Invoicing to aggregate an advertiser's charges for a billing period. */
    List<LedgerEntry> findByTenantIdAndCampaignIdInAndCreatedAtBetween(
            Long tenantId, List<Long> campaignIds, LocalDateTime start, LocalDateTime end);
}
