package com.adc.adcampaign.channels.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChannelToggleRequest {
    @NotNull
    private Boolean enabled;
}
