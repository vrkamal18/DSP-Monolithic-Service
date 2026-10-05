package com.adc.adcampaign.campaign.dto;

import com.adc.adcampaign.campaign.entity.Advertiser;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdvertiserResponse {
    private Long id;
    private String name;
    private String email;
    private String company;
    private LocalDateTime createdAt;

    public static AdvertiserResponse from(Advertiser advertiser) {
        return AdvertiserResponse.builder()
                .id(advertiser.getId())
                .name(advertiser.getName())
                .email(advertiser.getEmail())
                .company(advertiser.getCompany())
                .createdAt(advertiser.getCreatedAt())
                .build();
    }
}
