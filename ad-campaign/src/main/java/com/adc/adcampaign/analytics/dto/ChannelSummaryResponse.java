package com.adc.adcampaign.analytics.dto;

import com.adc.adcampaign.analytics.AnalyticsService;
import com.adc.adcampaign.connectors.entity.ConnectorStatus;
import com.adc.adcampaign.connectors.entity.ConnectorType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ChannelSummaryResponse {
    private Long connectorId;
    private ConnectorType type;
    private ConnectorStatus status;
    private long totalSyncs;
    private long successfulSyncs;
    private long failedSyncs;
    private LocalDateTime lastSyncedAt;

    public static ChannelSummaryResponse from(AnalyticsService.ChannelSyncSummary s) {
        return ChannelSummaryResponse.builder()
                .connectorId(s.connectorId())
                .type(s.type())
                .status(s.status())
                .totalSyncs(s.totalSyncs())
                .successfulSyncs(s.successfulSyncs())
                .failedSyncs(s.failedSyncs())
                .lastSyncedAt(s.lastSyncedAt())
                .build();
    }
}
