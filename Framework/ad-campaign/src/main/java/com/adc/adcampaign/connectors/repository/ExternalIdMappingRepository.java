package com.adc.adcampaign.connectors.repository;

import com.adc.adcampaign.connectors.entity.ExternalIdMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExternalIdMappingRepository extends JpaRepository<ExternalIdMapping, Long> {
    Optional<ExternalIdMapping> findByTenantIdAndConnectorIdAndCampaignId(
            Long tenantId, Long connectorId, Long campaignId);
}
