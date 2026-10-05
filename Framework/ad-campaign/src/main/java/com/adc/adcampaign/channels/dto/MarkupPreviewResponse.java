package com.adc.adcampaign.channels.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MarkupPreviewResponse {
    private Long creativeId;
    private String markup;
}
