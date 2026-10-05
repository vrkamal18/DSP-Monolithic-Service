package com.adc.adcampaign.administration;

import com.adc.adcampaign.identity.entity.AppUser;
import com.adc.adcampaign.identity.entity.Role;
import com.adc.adcampaign.identity.entity.UserRoleAssignment;
import com.adc.adcampaign.identity.repository.AppUserRepository;
import com.adc.adcampaign.identity.repository.UserRoleAssignmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PlatformAdminBootstrapTest {

    @Mock private AppUserRepository appUserRepository;
    @Mock private UserRoleAssignmentRepository userRoleAssignmentRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void grantsRoleWhenEmailMatchesAUserWithNoExistingAssignment() {
        AppUser user = AppUser.builder().id(5L).email("admin@example.com").build();
        when(appUserRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(user));
        when(userRoleAssignmentRepository.findByUserIdAndTenantIsNullAndRole(5L, Role.PLATFORM_ADMIN))
                .thenReturn(Optional.empty());

        new PlatformAdminBootstrap(appUserRepository, userRoleAssignmentRepository, "admin@example.com")
                .run(null);

        verify(userRoleAssignmentRepository).save(any(UserRoleAssignment.class));
    }

    @Test
    void isANoOpWhenAlreadyGranted() {
        AppUser user = AppUser.builder().id(5L).email("admin@example.com").build();
        when(appUserRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(user));
        when(userRoleAssignmentRepository.findByUserIdAndTenantIsNullAndRole(5L, Role.PLATFORM_ADMIN))
                .thenReturn(Optional.of(UserRoleAssignment.builder().id(1L).build()));

        new PlatformAdminBootstrap(appUserRepository, userRoleAssignmentRepository, "admin@example.com")
                .run(null);

        verify(userRoleAssignmentRepository, never()).save(any());
    }

    @Test
    void isANoOpWhenEmailDoesNotMatchAnyUser() {
        when(appUserRepository.findByEmailIgnoreCase("ghost@example.com")).thenReturn(Optional.empty());

        new PlatformAdminBootstrap(appUserRepository, userRoleAssignmentRepository, "ghost@example.com")
                .run(null);

        verifyNoInteractions(userRoleAssignmentRepository);
    }

    @Test
    void isANoOpWhenPropertyIsBlank() {
        new PlatformAdminBootstrap(appUserRepository, userRoleAssignmentRepository, "")
                .run(null);

        verifyNoInteractions(appUserRepository, userRoleAssignmentRepository);
    }
}
