package com.adc.adcampaign.administration;

import com.adc.adcampaign.administration.entity.AdminAuditAction;
import com.adc.adcampaign.administration.entity.AdminAuditLog;
import com.adc.adcampaign.administration.repository.AdminAuditLogRepository;
import com.adc.adcampaign.foundation.web.CorrelationIdFilter;
import org.springframework.stereotype.Service;

@Service
public class AdminAuditService {

    private final AdminAuditLogRepository adminAuditLogRepository;

    public AdminAuditService(AdminAuditLogRepository adminAuditLogRepository) {
        this.adminAuditLogRepository = adminAuditLogRepository;
    }

    public void record(Long actorUserId, Long actorTenantId, Long targetTenantId, Long targetUserId,
                        AdminAuditAction action, String details) {
        adminAuditLogRepository.save(AdminAuditLog.builder()
                .actorUserId(actorUserId)
                .actorTenantId(actorTenantId)
                .targetTenantId(targetTenantId)
                .targetUserId(targetUserId)
                .action(action)
                .details(details)
                .correlationId(CorrelationIdFilter.current())
                .build());
    }
}
