package com.adc.adcampaign.finance.invoicing;

import com.adc.adcampaign.campaign.entity.Advertiser;
import com.adc.adcampaign.finance.invoicing.entity.Invoice;
import com.adc.adcampaign.finance.invoicing.entity.InvoiceLineItem;
import com.adc.adcampaign.finance.invoicing.entity.InvoiceStatus;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InvoicePdfGeneratorTest {

    private final InvoicePdfGenerator generator = new InvoicePdfGenerator();

    @Test
    void generatesAValidPdfContainingTheAdvertiserNameAndTotal() throws IOException {
        Invoice invoice = Invoice.builder()
                .id(42L)
                .status(InvoiceStatus.DRAFT)
                .periodStart(LocalDate.of(2026, 1, 1))
                .periodEnd(LocalDate.of(2026, 1, 31))
                .totalAmount(BigDecimal.valueOf(123.45))
                .currency("USD")
                .build();
        Advertiser advertiser = Advertiser.builder().id(1L).name("Acme Advertiser").build();
        List<InvoiceLineItem> lineItems = List.of(
                InvoiceLineItem.builder().campaignId(5L).description("Spring Sale").amount(BigDecimal.valueOf(123.45)).build());

        byte[] pdf = generator.generate(invoice, advertiser, lineItems);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("%PDF-");

        try (PDDocument document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document);
            assertThat(text).contains("Acme Advertiser");
            assertThat(text).contains("Spring Sale");
            assertThat(text).contains("123.45");
        }
    }
}
