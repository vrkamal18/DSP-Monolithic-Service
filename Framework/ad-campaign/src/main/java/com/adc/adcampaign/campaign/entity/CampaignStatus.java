package com.adc.adcampaign.campaign.entity;

/**
 * Simplified from service/documents/03-domain-and-data-specification.md §7
 * (drops the READY state — see the phase-2 plan for why).
 */
public enum CampaignStatus {
    DRAFT,
    ACTIVE,
    PAUSED,
    COMPLETED,
    ARCHIVED
}
