package com.adc.adcampaign.administration.dto;

import com.adc.adcampaign.administration.PlatformAdministrationService;
import com.adc.adcampaign.connectors.entity.ConnectorStatus;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class PlatformHealthResponse {
    private int totalTenants;
    private long activeTenants;
    private long inactiveTenants;
    private long totalUsers;
    private long enabledUsers;
    private long disabledUsers;
    private long totalCampaigns;
    private Map<ConnectorStatus, Long> connectorsByStatus;

    public static PlatformHealthResponse from(PlatformAdministrationService.PlatformHealth h) {
        return PlatformHealthResponse.builder()
                .totalTenants(h.totalTenants())
                .activeTenants(h.activeTenants())
                .inactiveTenants(h.inactiveTenants())
                .totalUsers(h.totalUsers())
                .enabledUsers(h.enabledUsers())
                .disabledUsers(h.disabledUsers())
                .totalCampaigns(h.totalCampaigns())
                .connectorsByStatus(h.connectorsByStatus())
                .build();
    }
}
