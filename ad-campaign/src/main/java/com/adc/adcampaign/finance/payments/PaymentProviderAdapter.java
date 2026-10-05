package com.adc.adcampaign.finance.payments;

import com.adc.adcampaign.finance.payments.entity.PaymentMethod;

import java.math.BigDecimal;

/**
 * Port for the real payment provider (Stripe or similar), per ADR-015 in
 * service/documents/08-architecture-decisions.md — "ports/adapters with mock
 * development implementations; production providers configured
 * individually." No provider has been chosen yet, so only
 * {@link MockPaymentProviderAdapter} exists today.
 */
public interface PaymentProviderAdapter {

    ChargeResult charge(BigDecimal amount, PaymentMethod method);

    record ChargeResult(boolean success, String providerChargeId) {
    }
}
