package com.adc.adcampaign.connectors.entity;

/**
 * service/documents/02-module-specifications.md §11's source references:
 * DSP-Social, DMP connectors in DSP-Audience, OTT connectors in
 * DSP-AdChannel. Identity/payment/exchange adapters already have their own
 * dedicated modules elsewhere and aren't part of this registry.
 */
public enum ConnectorType {
    SOCIAL,
    DMP,
    OTT
}
