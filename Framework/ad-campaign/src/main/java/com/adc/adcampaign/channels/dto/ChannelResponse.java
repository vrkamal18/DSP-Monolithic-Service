package com.adc.adcampaign.channels.dto;

import com.adc.adcampaign.channels.entity.Channel;
import com.adc.adcampaign.channels.entity.ChannelType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChannelResponse {
    private ChannelType type;
    private boolean enabled;

    public static ChannelResponse from(Channel channel) {
        return ChannelResponse.builder()
                .type(channel.getType())
                .enabled(channel.isEnabled())
                .build();
    }
}
