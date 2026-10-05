package com.adc.adcampaign.administration;

import com.adc.adcampaign.administration.entity.AdminAuditAction;
import com.adc.adcampaign.foundation.error.NotFoundException;
import com.adc.adcampaign.identity.entity.AppUser;
import com.adc.adcampaign.identity.entity.Role;
import com.adc.adcampaign.identity.entity.Tenant;
import com.adc.adcampaign.identity.entity.UserRoleAssignment;
import com.adc.adcampaign.identity.repository.AppUserRepository;
import com.adc.adcampaign.identity.repository.TenantRepository;
import com.adc.adcampaign.identity.repository.UserRoleAssignmentRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Tenant-scoped user/role administration — see
 * service/documents/02-module-specifications.md §15 ("Tenant and user
 * administration", "Role and plan management").
 */
@Service
public class UserAdministrationService {

    private final TenantRepository tenantRepository;
    private final AppUserRepository appUserRepository;
    private final UserRoleAssignmentRepository userRoleAssignmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminAuditService adminAuditService;

    public UserAdministrationService(TenantRepository tenantRepository,
                                      AppUserRepository appUserRepository,
                                      UserRoleAssignmentRepository userRoleAssignmentRepository,
                                      PasswordEncoder passwordEncoder,
                                      AdminAuditService adminAuditService) {
        this.tenantRepository = tenantRepository;
        this.appUserRepository = appUserRepository;
        this.userRoleAssignmentRepository = userRoleAssignmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminAuditService = adminAuditService;
    }

    @Transactional
    public AppUser createUser(Long tenantId, Long actorUserId, String email, String password, Role role) {
        if (appUserRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new NotFoundException("Tenant not found: " + tenantId));

        AppUser user = appUserRepository.save(AppUser.builder()
                .tenant(tenant)
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .enabled(true)
                .build());

        userRoleAssignmentRepository.save(UserRoleAssignment.builder()
                .user(user).tenant(tenant).role(role).build());

        adminAuditService.record(actorUserId, tenantId, tenantId, user.getId(),
                AdminAuditAction.USER_CREATED, "Created user " + email + " with role " + role);
        return user;
    }

    public List<UserWithRoles> listUsers(Long tenantId) {
        List<AppUser> users = appUserRepository.findByTenantId(tenantId);
        Map<Long, List<Role>> rolesByUserId = userRoleAssignmentRepository.findByTenantId(tenantId).stream()
                .collect(Collectors.groupingBy(a -> a.getUser().getId(),
                        Collectors.mapping(UserRoleAssignment::getRole, Collectors.toList())));

        return users.stream()
                .map(u -> new UserWithRoles(u, rolesByUserId.getOrDefault(u.getId(), List.of())))
                .toList();
    }

    @Transactional
    public void grantRole(Long tenantId, Long actorUserId, Long targetUserId, Role role) {
        AppUser user = appUserRepository.findByIdAndTenantId(targetUserId, tenantId)
                .orElseThrow(() -> new NotFoundException("User not found: " + targetUserId));
        if (userRoleAssignmentRepository.findByUserIdAndTenantIdAndRole(targetUserId, tenantId, role).isPresent()) {
            return;
        }
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new NotFoundException("Tenant not found: " + tenantId));

        userRoleAssignmentRepository.save(UserRoleAssignment.builder()
                .user(user).tenant(tenant).role(role).build());
        adminAuditService.record(actorUserId, tenantId, tenantId, targetUserId,
                AdminAuditAction.USER_ROLE_GRANTED, "Granted role " + role);
    }

    @Transactional
    public void revokeRole(Long tenantId, Long actorUserId, Long targetUserId, Role role) {
        appUserRepository.findByIdAndTenantId(targetUserId, tenantId)
                .orElseThrow(() -> new NotFoundException("User not found: " + targetUserId));
        UserRoleAssignment assignment = userRoleAssignmentRepository
                .findByUserIdAndTenantIdAndRole(targetUserId, tenantId, role)
                .orElseThrow(() -> new NotFoundException("Role not assigned: " + role));

        if (role == Role.TENANT_ADMIN
                && userRoleAssignmentRepository.countByTenantIdAndRole(tenantId, Role.TENANT_ADMIN) <= 1) {
            throw new IllegalStateException("Cannot remove the tenant's last TENANT_ADMIN");
        }

        userRoleAssignmentRepository.delete(assignment);
        adminAuditService.record(actorUserId, tenantId, tenantId, targetUserId,
                AdminAuditAction.USER_ROLE_REVOKED, "Revoked role " + role);
    }

    @Transactional
    public void deactivateUser(Long tenantId, Long actorUserId, Long targetUserId) {
        if (actorUserId.equals(targetUserId)) {
            throw new IllegalStateException("Cannot deactivate your own account");
        }
        AppUser user = appUserRepository.findByIdAndTenantId(targetUserId, tenantId)
                .orElseThrow(() -> new NotFoundException("User not found: " + targetUserId));
        user.setEnabled(false);
        appUserRepository.save(user);
        adminAuditService.record(actorUserId, tenantId, tenantId, targetUserId,
                AdminAuditAction.USER_DEACTIVATED, "Deactivated user " + user.getEmail());
    }

    @Transactional
    public void reactivateUser(Long tenantId, Long actorUserId, Long targetUserId) {
        AppUser user = appUserRepository.findByIdAndTenantId(targetUserId, tenantId)
                .orElseThrow(() -> new NotFoundException("User not found: " + targetUserId));
        user.setEnabled(true);
        appUserRepository.save(user);
        adminAuditService.record(actorUserId, tenantId, tenantId, targetUserId,
                AdminAuditAction.USER_REACTIVATED, "Reactivated user " + user.getEmail());
    }

    public record UserWithRoles(AppUser user, List<Role> roles) {
    }
}
