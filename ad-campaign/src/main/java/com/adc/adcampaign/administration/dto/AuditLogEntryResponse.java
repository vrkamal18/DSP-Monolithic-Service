package com.adc.adcampaign.administration.dto;

import com.adc.adcampaign.administration.entity.AdminAuditAction;
import com.adc.adcampaign.administration.entity.AdminAuditLog;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AuditLogEntryResponse {
    private Long id;
    private Long actorUserId;
    private Long actorTenantId;
    private Long targetTenantId;
    private Long targetUserId;
    private AdminAuditAction action;
    private String details;
    private String correlationId;
    private LocalDateTime createdAt;

    public static AuditLogEntryResponse from(AdminAuditLog log) {
        return AuditLogEntryResponse.builder()
                .id(log.getId())
                .actorUserId(log.getActorUserId())
                .actorTenantId(log.getActorTenantId())
                .targetTenantId(log.getTargetTenantId())
                .targetUserId(log.getTargetUserId())
                .action(log.getAction())
                .details(log.getDetails())
                .correlationId(log.getCorrelationId())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
