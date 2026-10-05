package com.adc.adcampaign.connectors;

import com.adc.adcampaign.campaign.entity.Campaign;
import com.adc.adcampaign.campaign.repository.CampaignRepository;
import com.adc.adcampaign.connectors.entity.*;
import com.adc.adcampaign.connectors.repository.ConnectorActivityRepository;
import com.adc.adcampaign.connectors.repository.ConnectorRepository;
import com.adc.adcampaign.connectors.repository.ExternalIdMappingRepository;
import com.adc.adcampaign.foundation.error.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConnectorServiceTest {

    private static final Long TENANT_ID = 1L;
    private static final Long CONNECTOR_ID = 5L;
    private static final Long CAMPAIGN_ID = 10L;

    @Mock private ConnectorRepository connectorRepository;
    @Mock private ConnectorActivityRepository connectorActivityRepository;
    @Mock private ExternalIdMappingRepository externalIdMappingRepository;
    @Mock private CampaignRepository campaignRepository;
    @Mock private ConnectorAdapter connectorAdapter;

    private ConnectorService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new ConnectorService(connectorRepository, connectorActivityRepository,
                externalIdMappingRepository, campaignRepository, connectorAdapter);
        when(connectorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(externalIdMappingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private Connector connectedConnector() {
        return Connector.builder().id(CONNECTOR_ID).tenantId(TENANT_ID).type(ConnectorType.SOCIAL)
                .status(ConnectorStatus.CONNECTED).build();
    }

    @Test
    void connectCreatesAConnectedConnectorAndLogsActivity() {
        when(connectorRepository.findByTenantIdAndType(TENANT_ID, ConnectorType.SOCIAL))
                .thenReturn(Optional.empty());

        Connector result = service.connect(TENANT_ID, ConnectorType.SOCIAL, "acct-1", "secret");

        assertThat(result.getStatus()).isEqualTo(ConnectorStatus.CONNECTED);
        assertThat(result.getExternalAccountId()).isEqualTo("acct-1");
        assertThat(result.getConnectedAt()).isNotNull();

        ArgumentCaptor<ConnectorActivity> captor = ArgumentCaptor.forClass(ConnectorActivity.class);
        verify(connectorActivityRepository).save(captor.capture());
        assertThat(captor.getValue().getActivityType()).isEqualTo(ConnectorActivityType.CONNECT);
        assertThat(captor.getValue().isSuccess()).isTrue();
    }

    @Test
    void disconnectSetsStatusAndLogsActivity() {
        when(connectorRepository.findByIdAndTenantId(CONNECTOR_ID, TENANT_ID))
                .thenReturn(Optional.of(connectedConnector()));

        Connector result = service.disconnect(TENANT_ID, CONNECTOR_ID);

        assertThat(result.getStatus()).isEqualTo(ConnectorStatus.DISCONNECTED);
        ArgumentCaptor<ConnectorActivity> captor = ArgumentCaptor.forClass(ConnectorActivity.class);
        verify(connectorActivityRepository).save(captor.capture());
        assertThat(captor.getValue().getActivityType()).isEqualTo(ConnectorActivityType.DISCONNECT);
    }

    @Test
    void testRecordsResultAndLogsActivity() {
        Connector connector = connectedConnector();
        when(connectorRepository.findByIdAndTenantId(CONNECTOR_ID, TENANT_ID)).thenReturn(Optional.of(connector));
        when(connectorAdapter.testConnection(connector)).thenReturn(true);

        Connector result = service.test(TENANT_ID, CONNECTOR_ID);

        assertThat(result.getLastTestSuccessful()).isTrue();
        assertThat(result.getLastTestedAt()).isNotNull();
        ArgumentCaptor<ConnectorActivity> captor = ArgumentCaptor.forClass(ConnectorActivity.class);
        verify(connectorActivityRepository).save(captor.capture());
        assertThat(captor.getValue().getActivityType()).isEqualTo(ConnectorActivityType.TEST);
        assertThat(captor.getValue().isSuccess()).isTrue();
    }

    @Test
    void syncRecordsSyncedAtAndLogsActivityWithRecordCount() {
        Connector connector = connectedConnector();
        when(connectorRepository.findByIdAndTenantId(CONNECTOR_ID, TENANT_ID)).thenReturn(Optional.of(connector));
        when(connectorAdapter.sync(connector)).thenReturn(new ConnectorAdapter.SyncResult(true, 42));

        Connector result = service.sync(TENANT_ID, CONNECTOR_ID);

        assertThat(result.getLastSyncedAt()).isNotNull();
        ArgumentCaptor<ConnectorActivity> captor = ArgumentCaptor.forClass(ConnectorActivity.class);
        verify(connectorActivityRepository).save(captor.capture());
        assertThat(captor.getValue().getActivityType()).isEqualTo(ConnectorActivityType.SYNC);
        assertThat(captor.getValue().getDetails()).contains("42");
    }

    @Test
    void pushCampaignRejectedWhenConnectorNotConnected() {
        Connector connector = Connector.builder().id(CONNECTOR_ID).tenantId(TENANT_ID).type(ConnectorType.SOCIAL)
                .status(ConnectorStatus.DISCONNECTED).build();
        when(connectorRepository.findByIdAndTenantId(CONNECTOR_ID, TENANT_ID)).thenReturn(Optional.of(connector));

        assertThatThrownBy(() -> service.pushCampaign(TENANT_ID, CONNECTOR_ID, CAMPAIGN_ID))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void pushCampaignRejectedForCampaignFromAnotherTenant() {
        when(connectorRepository.findByIdAndTenantId(CONNECTOR_ID, TENANT_ID))
                .thenReturn(Optional.of(connectedConnector()));
        when(campaignRepository.findByIdAndTenantId(CAMPAIGN_ID, TENANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.pushCampaign(TENANT_ID, CONNECTOR_ID, CAMPAIGN_ID))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void pushCampaignCreatesExternalIdMappingOnSuccess() {
        Connector connector = connectedConnector();
        Campaign campaign = Campaign.builder().id(CAMPAIGN_ID).build();
        when(connectorRepository.findByIdAndTenantId(CONNECTOR_ID, TENANT_ID)).thenReturn(Optional.of(connector));
        when(campaignRepository.findByIdAndTenantId(CAMPAIGN_ID, TENANT_ID)).thenReturn(Optional.of(campaign));
        when(connectorAdapter.pushCampaign(connector, campaign)).thenReturn("ext-123");
        when(externalIdMappingRepository.findByTenantIdAndConnectorIdAndCampaignId(TENANT_ID, CONNECTOR_ID, CAMPAIGN_ID))
                .thenReturn(Optional.empty());

        ExternalIdMapping result = service.pushCampaign(TENANT_ID, CONNECTOR_ID, CAMPAIGN_ID);

        assertThat(result.getExternalCampaignId()).isEqualTo("ext-123");
        assertThat(result.getCampaignId()).isEqualTo(CAMPAIGN_ID);
    }

    @Test
    void pauseCampaignRejectedWhenNeverPushed() {
        when(connectorRepository.findByIdAndTenantId(CONNECTOR_ID, TENANT_ID))
                .thenReturn(Optional.of(connectedConnector()));
        when(externalIdMappingRepository.findByTenantIdAndConnectorIdAndCampaignId(TENANT_ID, CONNECTOR_ID, CAMPAIGN_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.pauseCampaign(TENANT_ID, CONNECTOR_ID, CAMPAIGN_ID))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void resumeCampaignRejectedWhenNeverPushed() {
        when(connectorRepository.findByIdAndTenantId(CONNECTOR_ID, TENANT_ID))
                .thenReturn(Optional.of(connectedConnector()));
        when(externalIdMappingRepository.findByTenantIdAndConnectorIdAndCampaignId(TENANT_ID, CONNECTOR_ID, CAMPAIGN_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resumeCampaign(TENANT_ID, CONNECTOR_ID, CAMPAIGN_ID))
                .isInstanceOf(NotFoundException.class);
    }
}
