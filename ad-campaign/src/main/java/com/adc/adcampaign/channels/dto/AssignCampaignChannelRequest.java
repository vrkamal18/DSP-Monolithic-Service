package com.adc.adcampaign.channels.dto;

import com.adc.adcampaign.channels.entity.ChannelType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssignCampaignChannelRequest {
    @NotNull
    private ChannelType channelType;
}
