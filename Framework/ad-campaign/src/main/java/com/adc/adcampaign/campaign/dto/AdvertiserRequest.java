package com.adc.adcampaign.campaign.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdvertiserRequest {
    @NotBlank
    private String name;

    private String email;

    private String company;
}
