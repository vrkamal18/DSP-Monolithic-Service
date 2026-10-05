package com.adc.adcampaign.administration;

import com.adc.adcampaign.identity.entity.AppUser;
import com.adc.adcampaign.identity.entity.Role;
import com.adc.adcampaign.identity.entity.Tenant;
import com.adc.adcampaign.identity.entity.UserRoleAssignment;
import com.adc.adcampaign.identity.repository.AppUserRepository;
import com.adc.adcampaign.identity.repository.TenantRepository;
import com.adc.adcampaign.identity.repository.UserRoleAssignmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class UserAdministrationServiceTest {

    private static final Long TENANT_ID = 1L;
    private static final Long ACTOR_ID = 10L;
    private static final Long TARGET_ID = 20L;

    @Mock private TenantRepository tenantRepository;
    @Mock private AppUserRepository appUserRepository;
    @Mock private UserRoleAssignmentRepository userRoleAssignmentRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AdminAuditService adminAuditService;

    private UserAdministrationService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new UserAdministrationService(tenantRepository, appUserRepository,
                userRoleAssignmentRepository, passwordEncoder, adminAuditService);
    }

    @Test
    void createUserRejectsDuplicateEmail() {
        when(appUserRepository.findByEmailIgnoreCase("dup@example.com"))
                .thenReturn(Optional.of(AppUser.builder().id(99L).build()));

        assertThatThrownBy(() -> service.createUser(TENANT_ID, ACTOR_ID, "dup@example.com", "password123",
                Role.CAMPAIGN_MANAGER))
                .isInstanceOf(ResponseStatusException.class);
        verify(adminAuditService, never()).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    void createUserSavesUserAndRoleAndAudits() {
        Tenant tenant = Tenant.builder().id(TENANT_ID).name("Acme").build();
        when(appUserRepository.findByEmailIgnoreCase("new@example.com")).thenReturn(Optional.empty());
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(appUserRepository.save(any())).thenAnswer(inv -> {
            AppUser u = inv.getArgument(0);
            u.setId(TARGET_ID);
            return u;
        });

        AppUser result = service.createUser(TENANT_ID, ACTOR_ID, "new@example.com", "password123",
                Role.CAMPAIGN_MANAGER);

        assertThat(result.getId()).isEqualTo(TARGET_ID);
        verify(userRoleAssignmentRepository).save(any(UserRoleAssignment.class));
        verify(adminAuditService).record(eq(ACTOR_ID), eq(TENANT_ID), eq(TENANT_ID), eq(TARGET_ID), any(), any());
    }

    @Test
    void grantRoleIsIdempotentWhenAlreadyAssigned() {
        AppUser user = AppUser.builder().id(TARGET_ID).build();
        when(appUserRepository.findByIdAndTenantId(TARGET_ID, TENANT_ID)).thenReturn(Optional.of(user));
        when(userRoleAssignmentRepository.findByUserIdAndTenantIdAndRole(TARGET_ID, TENANT_ID, Role.ANALYST))
                .thenReturn(Optional.of(UserRoleAssignment.builder().id(1L).build()));

        service.grantRole(TENANT_ID, ACTOR_ID, TARGET_ID, Role.ANALYST);

        verify(userRoleAssignmentRepository, never()).save(any());
        verify(adminAuditService, never()).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    void revokeRoleRejectedWhenRemovingLastTenantAdmin() {
        when(appUserRepository.findByIdAndTenantId(TARGET_ID, TENANT_ID))
                .thenReturn(Optional.of(AppUser.builder().id(TARGET_ID).build()));
        when(userRoleAssignmentRepository.findByUserIdAndTenantIdAndRole(TARGET_ID, TENANT_ID, Role.TENANT_ADMIN))
                .thenReturn(Optional.of(UserRoleAssignment.builder().id(1L).role(Role.TENANT_ADMIN).build()));
        when(userRoleAssignmentRepository.countByTenantIdAndRole(TENANT_ID, Role.TENANT_ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> service.revokeRole(TENANT_ID, ACTOR_ID, TARGET_ID, Role.TENANT_ADMIN))
                .isInstanceOf(IllegalStateException.class);
        verify(userRoleAssignmentRepository, never()).delete(any());
    }

    @Test
    void revokeRoleSucceedsWhenAnotherTenantAdminRemains() {
        when(appUserRepository.findByIdAndTenantId(TARGET_ID, TENANT_ID))
                .thenReturn(Optional.of(AppUser.builder().id(TARGET_ID).build()));
        UserRoleAssignment assignment = UserRoleAssignment.builder().id(1L).role(Role.TENANT_ADMIN).build();
        when(userRoleAssignmentRepository.findByUserIdAndTenantIdAndRole(TARGET_ID, TENANT_ID, Role.TENANT_ADMIN))
                .thenReturn(Optional.of(assignment));
        when(userRoleAssignmentRepository.countByTenantIdAndRole(TENANT_ID, Role.TENANT_ADMIN)).thenReturn(2L);

        service.revokeRole(TENANT_ID, ACTOR_ID, TARGET_ID, Role.TENANT_ADMIN);

        verify(userRoleAssignmentRepository).delete(assignment);
        verify(adminAuditService).record(eq(ACTOR_ID), eq(TENANT_ID), eq(TENANT_ID), eq(TARGET_ID), any(), any());
    }

    @Test
    void deactivateUserRejectsSelfDeactivation() {
        assertThatThrownBy(() -> service.deactivateUser(TENANT_ID, ACTOR_ID, ACTOR_ID))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(appUserRepository);
    }

    @Test
    void deactivateUserFlipsEnabledAndAudits() {
        AppUser user = AppUser.builder().id(TARGET_ID).email("x@example.com").enabled(true).build();
        when(appUserRepository.findByIdAndTenantId(TARGET_ID, TENANT_ID)).thenReturn(Optional.of(user));
        when(appUserRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.deactivateUser(TENANT_ID, ACTOR_ID, TARGET_ID);

        assertThat(user.isEnabled()).isFalse();
        verify(adminAuditService).record(eq(ACTOR_ID), eq(TENANT_ID), eq(TENANT_ID), eq(TARGET_ID), any(), any());
    }
}
