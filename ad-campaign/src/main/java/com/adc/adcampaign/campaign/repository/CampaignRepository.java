package com.adc.adcampaign.campaign.repository;

import com.adc.adcampaign.campaign.entity.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {
    List<Campaign> findByAdvertiserId(Long advertiserId);
    List<Campaign> findByTenantId(Long tenantId);
    Optional<Campaign> findByIdAndTenantId(Long id, Long tenantId);

    /**
     * A scalar projection, not {@code campaign.getAdvertiser().getId()}:
     * bidding.BiddingService's decisioning isn't wrapped in one
     * {@code @Transactional} (each repository call gets its own short-lived
     * session, deliberately, so a slow decision can't hold one DB connection
     * for the whole tmax window), so navigating a lazy association after an
     * earlier call's session has already closed throws
     * LazyInitializationException. This query resolves the id entirely
     * within its own call, with no proxy left to initialize afterward.
     */
    @Query("SELECT c.advertiser.id FROM Campaign c WHERE c.id = :campaignId")
    Optional<Long> findAdvertiserIdById(@Param("campaignId") Long campaignId);
}
