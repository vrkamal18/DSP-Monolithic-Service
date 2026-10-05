package com.adc.adcampaign.connectors.repository;

import com.adc.adcampaign.connectors.entity.Connector;
import com.adc.adcampaign.connectors.entity.ConnectorStatus;
import com.adc.adcampaign.connectors.entity.ConnectorType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConnectorRepository extends JpaRepository<Connector, Long> {
    List<Connector> findByTenantId(Long tenantId);
    Optional<Connector> findByIdAndTenantId(Long id, Long tenantId);
    Optional<Connector> findByTenantIdAndType(Long tenantId, ConnectorType type);
    long countByStatus(ConnectorStatus status);
}
