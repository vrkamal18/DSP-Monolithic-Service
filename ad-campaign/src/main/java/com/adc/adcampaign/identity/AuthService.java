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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final TenantRepository tenantRepository;
    private final AppUserRepository appUserRepository;
    private final UserRoleAssignmentRepository userRoleAssignmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(TenantRepository tenantRepository,
                        AppUserRepository appUserRepository,
                        UserRoleAssignmentRepository userRoleAssignmentRepository,
                        PasswordEncoder passwordEncoder,
                        JwtService jwtService) {
        this.tenantRepository = tenantRepository;
        this.appUserRepository = appUserRepository;
        this.userRoleAssignmentRepository = userRoleAssignmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        if (tenantRepository.findByName(request.getTenantName()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tenant name already in use");
        }
        if (appUserRepository.findByEmailIgnoreCase(request.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        Tenant tenant = tenantRepository.save(Tenant.builder().name(request.getTenantName()).build());

        AppUser user = appUserRepository.save(AppUser.builder()
                .tenant(tenant)
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .enabled(true)
                .build());

        userRoleAssignmentRepository.save(UserRoleAssignment.builder()
                .user(user)
                .tenant(tenant)
                .role(Role.TENANT_ADMIN)
                .build());

        return issueTokenFor(user, tenant.getId(), Set.of(Role.TENANT_ADMIN.name()));
    }

    public AuthResponse login(LoginRequest request) {
        AppUser user = appUserRepository.findByEmailIgnoreCase(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!user.isEnabled() || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }
        if (Boolean.FALSE.equals(user.getTenant().getActive())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        Set<String> roles = userRoleAssignmentRepository.findByUserId(user.getId()).stream()
                .map(assignment -> assignment.getRole().name())
                .collect(Collectors.toUnmodifiableSet());

        return issueTokenFor(user, user.getTenant().getId(), roles);
    }

    private AuthResponse issueTokenFor(AppUser user, Long tenantId, Set<String> roles) {
        JwtService.IssuedToken issued = jwtService.issue(user.getId(), tenantId, roles);
        return AuthResponse.builder()
                .token(issued.token())
                .expiresAt(issued.expiresAt())
                .tenantId(tenantId)
                .userId(user.getId())
                .build();
    }
}
