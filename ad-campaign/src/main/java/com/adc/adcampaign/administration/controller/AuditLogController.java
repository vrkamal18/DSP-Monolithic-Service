package com.adc.adcampaign.administration.controller;

import com.adc.adcampaign.administration.dto.AuditLogEntryResponse;
import com.adc.adcampaign.administration.repository.AdminAuditLogRepository;
import com.adc.adcampaign.foundation.security.TenantContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

/**
 * PLATFORM_ADMIN sees every audit row; TENANT_ADMIN sees only rows where
 * their own tenant is the actor or the target — never another tenant's
 * entries, per the "no impersonation" stance in the administration plan.
 */
@RestController
@RequestMapping("/api/v1/admin/audit-log")
@PreAuthorize("hasAnyRole('TENANT_ADMIN', 'PLATFORM_ADMIN')")
public class AuditLogController {

    private final AdminAuditLogRepository adminAuditLogRepository;

    public AuditLogController(AdminAuditLogRepository adminAuditLogRepository) {
        this.adminAuditLogRepository = adminAuditLogRepository;
    }

    @GetMapping
    public ResponseEntity<List<AuditLogEntryResponse>> list() {
        Set<String> roles = TenantContext.current().map(p -> p.roles()).orElse(Set.of());
        var entries = roles.contains("PLATFORM_ADMIN")
                ? adminAuditLogRepository.findAllByOrderByCreatedAtDesc()
                : adminAuditLogRepository.findByActorTenantIdOrTargetTenantIdOrderByCreatedAtDesc(
                        TenantContext.requireTenantId(), TenantContext.requireTenantId());

        return ResponseEntity.ok(entries.stream().map(AuditLogEntryResponse::from).toList());
    }
}
