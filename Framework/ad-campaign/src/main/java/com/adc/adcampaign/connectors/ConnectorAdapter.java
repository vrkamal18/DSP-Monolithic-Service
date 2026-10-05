package com.adc.adcampaign.connectors;

import com.adc.adcampaign.campaign.entity.Campaign;
import com.adc.adcampaign.connectors.entity.Connector;

/**
 * Port for the real social/DMP/OTT platform integrations — per ADR-015 in
 * service/documents/08-architecture-decisions.md, the same
 * "ports/adapters with mock development implementations" pattern already
 * used for {@code finance.payments.PaymentProviderAdapter} and
 * {@code finance.invoicing.InvoiceEmailAdapter}. No vendor has been chosen,
 * so only {@link MockConnectorAdapter} exists today.
 */
public interface ConnectorAdapter {

    boolean testConnection(Connector connector);

    SyncResult sync(Connector connector);

    String pushCampaign(Connector connector, Campaign campaign);

    void pauseCampaign(Connector connector, String externalCampaignId);

    void resumeCampaign(Connector connector, String externalCampaignId);

    record SyncResult(boolean success, int recordsSynced) {
    }
}
