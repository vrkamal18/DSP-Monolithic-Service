package com.adc.adcampaign.administration;

import com.adc.adcampaign.identity.entity.AppUser;
import com.adc.adcampaign.identity.entity.Role;
import com.adc.adcampaign.identity.entity.UserRoleAssignment;
import com.adc.adcampaign.identity.repository.AppUserRepository;
import com.adc.adcampaign.identity.repository.UserRoleAssignmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Grants PLATFORM_ADMIN to one operator-configured, already-registered user
 * on startup — the only way this role can ever be granted today, since no
 * code path creates it otherwise. Same "operator configures this externally"
 * pattern as {@code exchange.bidscube.BidsCubeProperties.tenantId}. No-ops
 * silently if the property is unset or the email isn't registered yet, so
 * startup never fails on a stale/pre-registration config.
 */
@Component
public class PlatformAdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PlatformAdminBootstrap.class);

    private final AppUserRepository appUserRepository;
    private final UserRoleAssignmentRepository userRoleAssignmentRepository;
    private final String bootstrapAdminEmail;

    public PlatformAdminBootstrap(AppUserRepository appUserRepository,
                                   UserRoleAssignmentRepository userRoleAssignmentRepository,
                                   @Value("${administration.bootstrap-admin-email:}") String bootstrapAdminEmail) {
        this.appUserRepository = appUserRepository;
        this.userRoleAssignmentRepository = userRoleAssignmentRepository;
        this.bootstrapAdminEmail = bootstrapAdminEmail;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (bootstrapAdminEmail == null || bootstrapAdminEmail.isBlank()) {
            return;
        }
        appUserRepository.findByEmailIgnoreCase(bootstrapAdminEmail).ifPresentOrElse(this::grantIfMissing,
                () -> log.info("administration.bootstrap-admin-email is set to '{}' but no such user is "
                        + "registered yet; skipping PLATFORM_ADMIN bootstrap", bootstrapAdminEmail));
    }

    private void grantIfMissing(AppUser user) {
        if (userRoleAssignmentRepository.findByUserIdAndTenantIsNullAndRole(user.getId(), Role.PLATFORM_ADMIN)
                .isPresent()) {
            return;
        }
        userRoleAssignmentRepository.save(UserRoleAssignment.builder()
                .user(user).tenant(null).role(Role.PLATFORM_ADMIN).build());
        log.info("Granted PLATFORM_ADMIN to {}", user.getEmail());
    }
}
