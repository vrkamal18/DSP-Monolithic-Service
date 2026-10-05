package com.adc.adcampaign.connectors.dto;

import com.adc.adcampaign.connectors.entity.ExternalIdMapping;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExternalIdMappingResponse {
    private Long campaignId;
    private String externalCampaignId;

    public static ExternalIdMappingResponse from(ExternalIdMapping mapping) {
        return ExternalIdMappingResponse.builder()
                .campaignId(mapping.getCampaignId())
                .externalCampaignId(mapping.getExternalCampaignId())
                .build();
    }
}
