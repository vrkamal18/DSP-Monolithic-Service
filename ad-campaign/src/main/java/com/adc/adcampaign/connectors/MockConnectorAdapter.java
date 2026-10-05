package com.adc.adcampaign.connectors;

import com.adc.adcampaign.campaign.entity.Campaign;
import com.adc.adcampaign.connectors.entity.Connector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MockConnectorAdapter implements ConnectorAdapter {

    private static final Logger log = LoggerFactory.getLogger(MockConnectorAdapter.class);

    @Override
    public boolean testConnection(Connector connector) {
        log.info("Mock {} connector test for account {}: OK", connector.getType(), connector.getExternalAccountId());
        return true;
    }

    @Override
    public SyncResult sync(Connector connector) {
        int recordsSynced = 10;
        log.info("Mock {} connector sync for account {}: {} records", connector.getType(),
                connector.getExternalAccountId(), recordsSynced);
        return new SyncResult(true, recordsSynced);
    }

    @Override
    public String pushCampaign(Connector connector, Campaign campaign) {
        String externalCampaignId = "mock_ext_campaign_" + UUID.randomUUID();
        log.info("Mock {} connector pushed campaign {} as {}", connector.getType(), campaign.getId(),
                externalCampaignId);
        return externalCampaignId;
    }

    @Override
    public void pauseCampaign(Connector connector, String externalCampaignId) {
        log.info("Mock {} connector paused external campaign {}", connector.getType(), externalCampaignId);
    }

    @Override
    public void resumeCampaign(Connector connector, String externalCampaignId) {
        log.info("Mock {} connector resumed external campaign {}", connector.getType(), externalCampaignId);
    }
}
