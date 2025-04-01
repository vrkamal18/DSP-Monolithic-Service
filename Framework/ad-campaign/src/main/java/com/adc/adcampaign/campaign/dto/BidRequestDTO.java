package com.adc.adcampaign.campaign.dto;

import lombok.Data;

@Data
public class BidRequestDTO {
    private String location;     // e.g., "US"
    private String ageGroup;     // e.g., "18-24"
    private String deviceType;   // e.g., "mobile", "desktop"
}

