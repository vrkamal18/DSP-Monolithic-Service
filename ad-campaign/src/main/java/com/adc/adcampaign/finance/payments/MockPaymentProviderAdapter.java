package com.adc.adcampaign.finance.payments;

import com.adc.adcampaign.finance.payments.entity.PaymentMethod;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Always-succeeds mock provider. Stands in until a real payment provider is
 * chosen (see {@link PaymentProviderAdapter}'s Javadoc).
 */
@Component
public class MockPaymentProviderAdapter implements PaymentProviderAdapter {

    @Override
    public ChargeResult charge(BigDecimal amount, PaymentMethod method) {
        return new ChargeResult(true, "mock_" + UUID.randomUUID());
    }
}
