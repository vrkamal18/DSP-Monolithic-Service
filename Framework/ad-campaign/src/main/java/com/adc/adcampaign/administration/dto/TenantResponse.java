package com.adc.adcampaign.administration.dto;

import com.adc.adcampaign.administration.PlatformAdministrationService;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TenantResponse {
    private Long id;
    private String name;
    private boolean active;
    private int userCount;
    private LocalDateTime createdAt;

    public static TenantResponse from(PlatformAdministrationService.TenantWithUserCount t) {
        return TenantResponse.builder()
                .id(t.tenant().getId())
                .name(t.tenant().getName())
                .active(!Boolean.FALSE.equals(t.tenant().getActive()))
                .userCount(t.userCount())
                .createdAt(t.tenant().getCreatedAt())
                .build();
    }
}
