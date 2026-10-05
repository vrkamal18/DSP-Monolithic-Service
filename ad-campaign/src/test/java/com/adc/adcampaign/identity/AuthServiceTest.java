package com.adc.adcampaign.identity;

import com.adc.adcampaign.identity.dto.AuthResponse;
import com.adc.adcampaign.identity.dto.LoginRequest;
import com.adc.adcampaign.identity.dto.RegisterRequest;
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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    @Mock private TenantRepository tenantRepository;
    @Mock private AppUserRepository appUserRepository;
    @Mock private UserRoleAssignmentRepository userRoleAssignmentRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        authService = new AuthService(tenantRepository, appUserRepository, userRoleAssignmentRepository,
                passwordEncoder, jwtService);
    }

    @Test
    void registerCreatesTenantAndTenantAdminAndIssuesToken() {
        RegisterRequest request = new RegisterRequest();
        request.setTenantName("acme");
        request.setEmail("owner@acme.com");
        request.setPassword("password123");

        when(tenantRepository.findByName("acme")).thenReturn(Optional.empty());
        when(appUserRepository.findByEmailIgnoreCase("owner@acme.com")).thenReturn(Optional.empty());
        when(tenantRepository.save(any())).thenAnswer(inv -> {
            Tenant t = inv.getArgument(0);
            t.setId(100L);
            return t;
        });
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(appUserRepository.save(any())).thenAnswer(inv -> {
            AppUser u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(jwtService.issue(eq(1L), eq(100L), anySet()))
                .thenReturn(new JwtService.IssuedToken("token-value", Instant.now().plusSeconds(3600)));

        AuthResponse response = authService.register(request);

        assertThat(response.getToken()).isEqualTo("token-value");
        assertThat(response.getTenantId()).isEqualTo(100L);
        assertThat(response.getUserId()).isEqualTo(1L);
        verify(userRoleAssignmentRepository).save(argThatRoleIs(Role.TENANT_ADMIN));
    }

    @Test
    void registerRejectsADuplicateTenantName() {
        RegisterRequest request = new RegisterRequest();
        request.setTenantName("acme");
        request.setEmail("owner@acme.com");
        request.setPassword("password123");

        when(tenantRepository.findByName("acme")).thenReturn(Optional.of(Tenant.builder().id(1L).name("acme").build()));

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void loginSucceedsWithMatchingPassword() {
        Tenant tenant = Tenant.builder().id(100L).name("acme").build();
        AppUser user = AppUser.builder().id(1L).tenant(tenant).email("owner@acme.com")
                .passwordHash("hashed").enabled(true).build();

        LoginRequest request = new LoginRequest();
        request.setEmail("owner@acme.com");
        request.setPassword("password123");

        when(appUserRepository.findByEmailIgnoreCase("owner@acme.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(userRoleAssignmentRepository.findByUserId(1L)).thenReturn(List.of(
                UserRoleAssignment.builder().user(user).tenant(tenant).role(Role.TENANT_ADMIN).build()));
        when(jwtService.issue(eq(1L), eq(100L), any()))
                .thenReturn(new JwtService.IssuedToken("token-value", Instant.now().plusSeconds(3600)));

        AuthResponse response = authService.login(request);

        assertThat(response.getToken()).isEqualTo("token-value");
        assertThat(response.getTenantId()).isEqualTo(100L);
    }

    @Test
    void loginFailsWithWrongPassword() {
        AppUser user = AppUser.builder().id(1L)
                .tenant(Tenant.builder().id(100L).build())
                .email("owner@acme.com").passwordHash("hashed").enabled(true).build();

        LoginRequest request = new LoginRequest();
        request.setEmail("owner@acme.com");
        request.setPassword("wrong");

        when(appUserRepository.findByEmailIgnoreCase("owner@acme.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request)).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void loginFailsWhenUserDoesNotExist() {
        LoginRequest request = new LoginRequest();
        request.setEmail("nobody@acme.com");
        request.setPassword("whatever");

        when(appUserRepository.findByEmailIgnoreCase("nobody@acme.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request)).isInstanceOf(BadCredentialsException.class);
    }

    private static UserRoleAssignment argThatRoleIs(Role role) {
        return argThat(assignment -> assignment.getRole() == role);
    }
}
