package com.adc.adcampaign.campaign.entity;

/**
 * Review workflow states from
 * service/documents/03-domain-and-data-specification.md §7.
 */
public enum CreativeStatus {
    DRAFT,
    PENDING_REVIEW,
    APPROVED,
    REJECTED,
    ARCHIVED
}
