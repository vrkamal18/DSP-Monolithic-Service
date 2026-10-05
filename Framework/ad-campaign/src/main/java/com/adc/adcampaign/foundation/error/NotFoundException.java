package com.adc.adcampaign.foundation.error;

/**
 * Thrown when a requested resource doesn't exist or isn't visible to the
 * caller's tenant. Deliberately the same exception for both cases — never
 * reveal whether a resource exists in another tenant.
 */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
