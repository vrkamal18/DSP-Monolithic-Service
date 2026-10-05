package com.adc.adcampaign.campaign.dto;

import com.adc.adcampaign.campaign.entity.Creative;
import com.adc.adcampaign.campaign.entity.CreativeStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CreativeResponse {
    private Long id;
    private Long campaignId;
    private String name;
    private String type;
    private String url;
    private Integer width;
    private Integer height;
    private CreativeStatus status;
    private String rejectionReason;
    private Long reviewedByUserId;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;

    public static CreativeResponse from(Creative creative) {
        return CreativeResponse.builder()
                .id(creative.getId())
                .campaignId(creative.getCampaign() != null ? creative.getCampaign().getId() : null)
                .name(creative.getName())
                .type(creative.getType())
                .url(creative.getUrl())
                .width(creative.getWidth())
                .height(creative.getHeight())
                .status(creative.getStatus())
                .rejectionReason(creative.getRejectionReason())
                .reviewedByUserId(creative.getReviewedByUserId())
                .reviewedAt(creative.getReviewedAt())
                .createdAt(creative.getCreatedAt())
                .build();
    }
}
