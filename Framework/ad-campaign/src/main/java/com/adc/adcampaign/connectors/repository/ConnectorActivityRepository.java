package com.adc.adcampaign.connectors.repository;

import com.adc.adcampaign.connectors.entity.ConnectorActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConnectorActivityRepository extends JpaRepository<ConnectorActivity, Long> {
    List<ConnectorActivity> findByTenantIdAndConnectorId(Long tenantId, Long connectorId);
}
