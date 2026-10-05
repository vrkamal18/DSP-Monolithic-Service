package com.adc.adcampaign.administration;

import com.adc.adcampaign.campaign.repository.CampaignRepository;
import com.adc.adcampaign.connectors.entity.ConnectorStatus;
import com.adc.adcampaign.connectors.repository.ConnectorRepository;
import com.adc.adcampaign.identity.entity.AppUser;
import com.adc.adcampaign.identity.entity.Tenant;
import com.adc.adcampaign.identity.repository.AppUserRepository;
import com.adc.adcampaign.identity.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlatformAdministrationServiceTest {

    private static final Long ACTOR_ID = 1L;
    private static final Long ACTOR_TENANT_ID = 1L;
    private static final Long TARGET_TENANT_ID = 2L;

    @Mock private TenantRepository tenantRepository;
    @Mock private AppUserRepository appUserRepository;
    @Mock private CampaignRepository campaignRepository;
    @Mock private ConnectorRepository connectorRepository;
    @Mock private AdminAuditService adminAuditService;

    private PlatformAdministrationService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new PlatformAdministrationService(tenantRepository, appUserRepository, campaignRepository,
                connectorRepository, adminAuditService);
    }

    @Test
    void listTenantsIncludesPerTenantUserCounts() {
        Tenant t1 = Tenant.builder().id(1L).name("A").build();
        Tenant t2 = Tenant.builder().id(2L).name("B").build();
        when(tenantRepository.findAll()).thenReturn(List.of(t1, t2));
        when(appUserRepository.findByTenantId(1L)).thenReturn(List.of(AppUser.builder().id(1L).build(),
                AppUser.builder().id(2L).build()));
        when(appUserRepository.findByTenantId(2L)).thenReturn(List.of(AppUser.builder().id(3L).build()));

        var result = service.listTenants();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).userCount()).isEqualTo(2);
        assertThat(result.get(1).userCount()).isEqualTo(1);
    }

    @Test
    void deactivateTenantFlipsActiveAndAudits() {
        Tenant tenant = Tenant.builder().id(TARGET_TENANT_ID).name("B").build();
        when(tenantRepository.findById(TARGET_TENANT_ID)).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.deactivateTenant(ACTOR_ID, ACTOR_TENANT_ID, TARGET_TENANT_ID);

        assertThat(tenant.getActive()).isFalse();
        verify(adminAuditService).record(ACTOR_ID, ACTOR_TENANT_ID, TARGET_TENANT_ID, null,
                com.adc.adcampaign.administration.entity.AdminAuditAction.TENANT_DEACTIVATED,
                "Deactivated tenant B");
    }

    @Test
    void reactivateTenantFlipsActiveBackAndAudits() {
        Tenant tenant = Tenant.builder().id(TARGET_TENANT_ID).name("B").active(false).build();
        when(tenantRepository.findById(TARGET_TENANT_ID)).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.reactivateTenant(ACTOR_ID, ACTOR_TENANT_ID, TARGET_TENANT_ID);

        assertThat(tenant.getActive()).isTrue();
    }

    @Test
    void platformHealthSumsCountsCorrectly() {
        Tenant active = Tenant.builder().id(1L).active(true).build();
        Tenant inactive = Tenant.builder().id(2L).active(false).build();
        when(tenantRepository.findAll()).thenReturn(List.of(active, inactive));
        when(appUserRepository.count()).thenReturn(5L);
        when(appUserRepository.findAll()).thenReturn(List.of(
                AppUser.builder().id(1L).enabled(true).build(),
                AppUser.builder().id(2L).enabled(true).build(),
                AppUser.builder().id(3L).enabled(false).build()));
        when(campaignRepository.count()).thenReturn(42L);
        for (ConnectorStatus status : ConnectorStatus.values()) {
            when(connectorRepository.countByStatus(status)).thenReturn(status == ConnectorStatus.CONNECTED ? 3L : 0L);
        }

        var health = service.getPlatformHealth();

        assertThat(health.totalTenants()).isEqualTo(2);
        assertThat(health.activeTenants()).isEqualTo(1);
        assertThat(health.inactiveTenants()).isEqualTo(1);
        assertThat(health.totalUsers()).isEqualTo(5);
        assertThat(health.totalCampaigns()).isEqualTo(42);
        assertThat(health.connectorsByStatus().get(ConnectorStatus.CONNECTED)).isEqualTo(3L);
    }
}
