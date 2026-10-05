package com.adc.adcampaign.foundation.security;

import java.util.Set;

/**
 * The authenticated principal for management API requests: who they are,
 * which tenant they belong to, and what roles they hold. Set as the
 * Authentication principal by identity's JwtAuthFilter; read anywhere via
 * {@link TenantContext}.
 */
public record TenantPrincipal(Long userId, Long tenantId, Set<String> roles) {
}
