package com.adc.adcampaign.campaign.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RejectCreativeRequest {
    @NotBlank
    private String reason;
}
