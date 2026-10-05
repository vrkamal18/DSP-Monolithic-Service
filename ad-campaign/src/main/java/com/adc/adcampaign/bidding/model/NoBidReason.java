package com.adc.adcampaign.bidding.model;

/**
 * Internal no-bid reason codes. Never exposed to the calling exchange —
 * only used for the decision audit log and metrics.
 */
public enum NoBidReason {
    INVALID_REQUEST,
    UNSUPPORTED_MEDIA,
    UNSUPPORTED_CURRENCY,
    NO_ACTIVE_CAMPAIGN,
    TARGETING_MISMATCH,
    NO_ELIGIBLE_CREATIVE,
    FREQUENCY_CAPPED,
    PACING_LIMITED,
    BUDGET_UNAVAILABLE,
    DEADLINE_EXCEEDED,
    INTERNAL_ERROR
}
