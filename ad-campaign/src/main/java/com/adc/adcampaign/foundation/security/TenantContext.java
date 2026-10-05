package com.adc.adcampaign.foundation.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Reads the current request's tenant/user/roles from Spring Security's
 * context. Business code calls this instead of trusting a tenantId/userId
 * supplied in a request body, per
 * service/documents/03-domain-and-data-specification.md §4 rule 1.
 */
public final class TenantContext {

    private TenantContext() {
    }

    public static Optional<TenantPrincipal> current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof TenantPrincipal principal)) {
            return Optional.empty();
        }
        return Optional.of(principal);
    }

    public static Long requireTenantId() {
        return current()
                .map(TenantPrincipal::tenantId)
                .orElseThrow(() -> new AccessDeniedException("No authenticated tenant context"));
    }

    public static Long requireUserId() {
        return current()
                .map(TenantPrincipal::userId)
                .orElseThrow(() -> new AccessDeniedException("No authenticated user context"));
    }
}
