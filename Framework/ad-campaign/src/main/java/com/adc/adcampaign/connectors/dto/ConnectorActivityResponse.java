package com.adc.adcampaign.connectors.dto;

import com.adc.adcampaign.connectors.entity.ConnectorActivity;
import com.adc.adcampaign.connectors.entity.ConnectorActivityType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ConnectorActivityResponse {
    private ConnectorActivityType activityType;
    private boolean success;
    private String details;
    private LocalDateTime createdAt;

    public static ConnectorActivityResponse from(ConnectorActivity activity) {
        return ConnectorActivityResponse.builder()
                .activityType(activity.getActivityType())
                .success(activity.isSuccess())
                .details(activity.getDetails())
                .createdAt(activity.getCreatedAt())
                .build();
    }
}
