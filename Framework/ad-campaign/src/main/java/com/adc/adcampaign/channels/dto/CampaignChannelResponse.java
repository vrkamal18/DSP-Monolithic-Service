package com.adc.adcampaign.channels.dto;

import com.adc.adcampaign.channels.entity.CampaignChannel;
import com.adc.adcampaign.channels.entity.ChannelType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CampaignChannelResponse {
    private ChannelType channelType;

    public static CampaignChannelResponse from(CampaignChannel campaignChannel) {
        return CampaignChannelResponse.builder()
                .channelType(campaignChannel.getChannelType())
                .build();
    }
}
