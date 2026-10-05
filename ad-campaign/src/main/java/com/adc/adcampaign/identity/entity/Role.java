package com.adc.adcampaign.identity.entity;

/**
 * Roles from service/documents/06-security-and-multitenancy.md §4.
 * PLATFORM_ADMIN and SERVICE_PRINCIPAL are not tenant-scoped; every other
 * role is granted within exactly one tenant.
 */
public enum Role {
    PLATFORM_ADMIN,
    TENANT_ADMIN,
    CAMPAIGN_MANAGER,
    CREATIVE_REVIEWER,
    FINANCE_USER,
    ANALYST,
    SERVICE_PRINCIPAL
}
