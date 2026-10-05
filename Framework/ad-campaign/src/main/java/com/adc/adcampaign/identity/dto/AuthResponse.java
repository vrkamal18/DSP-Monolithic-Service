package com.adc.adcampaign.identity.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class AuthResponse {
    private String token;
    private Instant expiresAt;
    private Long tenantId;
    private Long userId;
}
