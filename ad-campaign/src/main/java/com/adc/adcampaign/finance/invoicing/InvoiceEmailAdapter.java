package com.adc.adcampaign.finance.invoicing;

/**
 * Port for real email delivery (SMTP or a transactional-email provider) —
 * same "ports/adapters with mock development implementations" pattern
 * (ADR-015) already used for {@code finance.payments.PaymentProviderAdapter}.
 * No mail server is configured yet, so only {@link MockInvoiceEmailAdapter}
 * exists today.
 */
public interface InvoiceEmailAdapter {
    void send(String toEmail, String subject, byte[] pdfAttachment);
}
