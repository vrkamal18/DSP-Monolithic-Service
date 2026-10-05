package com.adc.adcampaign.connectors.dto;

import com.adc.adcampaign.connectors.entity.Connector;
import com.adc.adcampaign.connectors.entity.ConnectorStatus;
import com.adc.adcampaign.connectors.entity.ConnectorType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** Deliberately has no credential field — never echo the secret back. */
@Data
@Builder
public class ConnectorResponse {
    private Long id;
    private ConnectorType type;
    private ConnectorStatus status;
    private String externalAccountId;
    private LocalDateTime connectedAt;
    private LocalDateTime lastSyncedAt;
    private LocalDateTime lastTestedAt;
    private Boolean lastTestSuccessful;

    public static ConnectorResponse from(Connector connector) {
        return ConnectorResponse.builder()
                .id(connector.getId())
                .type(connector.getType())
                .status(connector.getStatus())
                .externalAccountId(connector.getExternalAccountId())
                .connectedAt(connector.getConnectedAt())
                .lastSyncedAt(connector.getLastSyncedAt())
                .lastTestedAt(connector.getLastTestedAt())
                .lastTestSuccessful(connector.getLastTestSuccessful())
                .build();
    }
}
