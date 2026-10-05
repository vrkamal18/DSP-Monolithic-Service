package com.adc.adcampaign.administration;

import com.adc.adcampaign.administration.entity.AdminAuditAction;
import com.adc.adcampaign.campaign.repository.CampaignRepository;
import com.adc.adcampaign.connectors.entity.ConnectorStatus;
import com.adc.adcampaign.connectors.repository.ConnectorRepository;
import com.adc.adcampaign.foundation.error.NotFoundException;
import com.adc.adcampaign.identity.entity.Tenant;
import com.adc.adcampaign.identity.repository.AppUserRepository;
import com.adc.adcampaign.identity.repository.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Cross-tenant platform administration — see
 * service/documents/02-module-specifications.md §15 ("Tenant...
 * administration", "Connector/platform status", "Operational health
 * summary"). Callers are authorized purely by PLATFORM_ADMIN role, never by
 * tenant scoping (these endpoints operate across every tenant by design).
 */
@Service
public class PlatformAdministrationService {

    private final TenantRepository tenantRepository;
    private final AppUserRepository appUserRepository;
    private final CampaignRepository campaignRepository;
    private final ConnectorRepository connectorRepository;
    private final AdminAuditService adminAuditService;

    public PlatformAdministrationService(TenantRepository tenantRepository,
                                          AppUserRepository appUserRepository,
                                          CampaignRepository campaignRepository,
                                          ConnectorRepository connectorRepository,
                                          AdminAuditService adminAuditService) {
        this.tenantRepository = tenantRepository;
        this.appUserRepository = appUserRepository;
        this.campaignRepository = campaignRepository;
        this.connectorRepository = connectorRepository;
        this.adminAuditService = adminAuditService;
    }

    public List<TenantWithUserCount> listTenants() {
        return tenantRepository.findAll().stream()
                .map(t -> new TenantWithUserCount(t, appUserRepository.findByTenantId(t.getId()).size()))
                .toList();
    }

    @Transactional
    public void deactivateTenant(Long actorUserId, Long actorTenantId, Long targetTenantId) {
        Tenant tenant = tenantRepository.findById(targetTenantId)
                .orElseThrow(() -> new NotFoundException("Tenant not found: " + targetTenantId));
        tenant.setActive(false);
        tenantRepository.save(tenant);
        adminAuditService.record(actorUserId, actorTenantId, targetTenantId, null,
                AdminAuditAction.TENANT_DEACTIVATED, "Deactivated tenant " + tenant.getName());
    }

    @Transactional
    public void reactivateTenant(Long actorUserId, Long actorTenantId, Long targetTenantId) {
        Tenant tenant = tenantRepository.findById(targetTenantId)
                .orElseThrow(() -> new NotFoundException("Tenant not found: " + targetTenantId));
        tenant.setActive(true);
        tenantRepository.save(tenant);
        adminAuditService.record(actorUserId, actorTenantId, targetTenantId, null,
                AdminAuditAction.TENANT_REACTIVATED, "Reactivated tenant " + tenant.getName());
    }

    public PlatformHealth getPlatformHealth() {
        List<Tenant> tenants = tenantRepository.findAll();
        long activeTenants = tenants.stream().filter(t -> !Boolean.FALSE.equals(t.getActive())).count();
        long inactiveTenants = tenants.size() - activeTenants;

        long totalUsers = appUserRepository.count();
        long enabledUsers = appUserRepository.findAll().stream().filter(u -> u.isEnabled()).count();
        long disabledUsers = totalUsers - enabledUsers;

        Map<ConnectorStatus, Long> connectorsByStatus = new EnumMap<>(ConnectorStatus.class);
        for (ConnectorStatus status : ConnectorStatus.values()) {
            connectorsByStatus.put(status, connectorRepository.countByStatus(status));
        }

        return new PlatformHealth(tenants.size(), activeTenants, inactiveTenants,
                totalUsers, enabledUsers, disabledUsers, campaignRepository.count(), connectorsByStatus);
    }

    public record TenantWithUserCount(Tenant tenant, int userCount) {
    }

    public record PlatformHealth(int totalTenants, long activeTenants, long inactiveTenants,
                                  long totalUsers, long enabledUsers, long disabledUsers,
                                  long totalCampaigns, Map<ConnectorStatus, Long> connectorsByStatus) {
    }
}
