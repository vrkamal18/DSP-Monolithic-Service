package com.adc.adcampaign.finance.invoicing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MockInvoiceEmailAdapter implements InvoiceEmailAdapter {

    private static final Logger log = LoggerFactory.getLogger(MockInvoiceEmailAdapter.class);

    @Override
    public void send(String toEmail, String subject, byte[] pdfAttachment) {
        log.info("Mock email: would send '{}' to {} with a {}-byte PDF attachment", subject, toEmail,
                pdfAttachment.length);
    }
}
