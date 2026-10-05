package com.adc.adcampaign.connectors;

import com.adc.adcampaign.campaign.entity.Campaign;
import com.adc.adcampaign.campaign.repository.CampaignRepository;
import com.adc.adcampaign.connectors.entity.*;
import com.adc.adcampaign.connectors.repository.ConnectorActivityRepository;
import com.adc.adcampaign.connectors.repository.ConnectorRepository;
import com.adc.adcampaign.connectors.repository.ExternalIdMappingRepository;
import com.adc.adcampaign.foundation.error.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ConnectorService {

    private final ConnectorRepository connectorRepository;
    private final ConnectorActivityRepository connectorActivityRepository;
    private final ExternalIdMappingRepository externalIdMappingRepository;
    private final CampaignRepository campaignRepository;
    private final ConnectorAdapter connectorAdapter;

    public ConnectorService(ConnectorRepository connectorRepository,
                             ConnectorActivityRepository connectorActivityRepository,
                             ExternalIdMappingRepository externalIdMappingRepository,
                             CampaignRepository campaignRepository,
                             ConnectorAdapter connectorAdapter) {
        this.connectorRepository = connectorRepository;
        this.connectorActivityRepository = connectorActivityRepository;
        this.externalIdMappingRepository = externalIdMappingRepository;
        this.campaignRepository = campaignRepository;
        this.connectorAdapter = connectorAdapter;
    }

    @Transactional
    public Connector connect(Long tenantId, ConnectorType type, String externalAccountId, String credential) {
        Connector connector = connectorRepository.findByTenantIdAndType(tenantId, type)
                .orElseGet(() -> Connector.builder().tenantId(tenantId).type(type).build());
        connector.setExternalAccountId(externalAccountId);
        connector.setCredentialRef(credential);
        connector.setStatus(ConnectorStatus.CONNECTED);
        connector.setConnectedAt(LocalDateTime.now());
        connector = connectorRepository.save(connector);

        logActivity(tenantId, connector.getId(), ConnectorActivityType.CONNECT, true,
                "Connected " + type + " account " + externalAccountId);
        return connector;
    }

    @Transactional
    public Connector disconnect(Long tenantId, Long id) {
        Connector connector = getById(tenantId, id);
        connector.setStatus(ConnectorStatus.DISCONNECTED);
        connector = connectorRepository.save(connector);

        logActivity(tenantId, id, ConnectorActivityType.DISCONNECT, true, "Disconnected");
        return connector;
    }

    @Transactional
    public Connector test(Long tenantId, Long id) {
        Connector connector = getById(tenantId, id);
        boolean success = connectorAdapter.testConnection(connector);
        connector.setLastTestedAt(LocalDateTime.now());
        connector.setLastTestSuccessful(success);
        connector = connectorRepository.save(connector);

        logActivity(tenantId, id, ConnectorActivityType.TEST, success, success ? "Test succeeded" : "Test failed");
        return connector;
    }

    @Transactional
    public Connector sync(Long tenantId, Long id) {
        Connector connector = getById(tenantId, id);
        ConnectorAdapter.SyncResult result = connectorAdapter.sync(connector);
        connector.setLastSyncedAt(LocalDateTime.now());
        connector = connectorRepository.save(connector);

        logActivity(tenantId, id, ConnectorActivityType.SYNC, result.success(),
                "Synced " + result.recordsSynced() + " records");
        return connector;
    }

    @Transactional
    public ExternalIdMapping pushCampaign(Long tenantId, Long connectorId, Long campaignId) {
        Connector connector = getById(tenantId, connectorId);
        if (connector.getStatus() != ConnectorStatus.CONNECTED) {
            throw new IllegalStateException("Connector is not connected: " + connectorId);
        }
        Campaign campaign = campaignRepository.findByIdAndTenantId(campaignId, tenantId)
                .orElseThrow(() -> new NotFoundException("Campaign not found: " + campaignId));

        String externalCampaignId = connectorAdapter.pushCampaign(connector, campaign);

        ExternalIdMapping mapping = externalIdMappingRepository
                .findByTenantIdAndConnectorIdAndCampaignId(tenantId, connectorId, campaignId)
                .orElseGet(() -> ExternalIdMapping.builder()
                        .tenantId(tenantId).connectorId(connectorId).campaignId(campaignId).build());
        mapping.setExternalCampaignId(externalCampaignId);
        mapping = externalIdMappingRepository.save(mapping);

        logActivity(tenantId, connectorId, ConnectorActivityType.PUSH_CAMPAIGN, true,
                "Pushed campaign " + campaignId + " as " + externalCampaignId);
        return mapping;
    }

    @Transactional
    public void pauseCampaign(Long tenantId, Long connectorId, Long campaignId) {
        Connector connector = getById(tenantId, connectorId);
        ExternalIdMapping mapping = requireMapping(tenantId, connectorId, campaignId);
        connectorAdapter.pauseCampaign(connector, mapping.getExternalCampaignId());
        logActivity(tenantId, connectorId, ConnectorActivityType.PAUSE_CAMPAIGN, true,
                "Paused campaign " + campaignId);
    }

    @Transactional
    public void resumeCampaign(Long tenantId, Long connectorId, Long campaignId) {
        Connector connector = getById(tenantId, connectorId);
        ExternalIdMapping mapping = requireMapping(tenantId, connectorId, campaignId);
        connectorAdapter.resumeCampaign(connector, mapping.getExternalCampaignId());
        logActivity(tenantId, connectorId, ConnectorActivityType.RESUME_CAMPAIGN, true,
                "Resumed campaign " + campaignId);
    }

    public Connector getById(Long tenantId, Long id) {
        return connectorRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Connector not found: " + id));
    }

    public List<Connector> listForTenant(Long tenantId) {
        return connectorRepository.findByTenantId(tenantId);
    }

    public List<ConnectorActivity> listActivity(Long tenantId, Long connectorId) {
        return connectorActivityRepository.findByTenantIdAndConnectorId(tenantId, connectorId);
    }

    private ExternalIdMapping requireMapping(Long tenantId, Long connectorId, Long campaignId) {
        return externalIdMappingRepository.findByTenantIdAndConnectorIdAndCampaignId(tenantId, connectorId, campaignId)
                .orElseThrow(() -> new NotFoundException(
                        "Campaign " + campaignId + " was never pushed to connector " + connectorId));
    }

    private void logActivity(Long tenantId, Long connectorId, ConnectorActivityType type, boolean success,
                              String details) {
        connectorActivityRepository.save(ConnectorActivity.builder()
                .tenantId(tenantId)
                .connectorId(connectorId)
                .activityType(type)
                .success(success)
                .details(details)
                .build());
    }
}
