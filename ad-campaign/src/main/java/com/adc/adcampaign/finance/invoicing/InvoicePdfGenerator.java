package com.adc.adcampaign.finance.invoicing;

import com.adc.adcampaign.campaign.entity.Advertiser;
import com.adc.adcampaign.finance.invoicing.entity.Invoice;
import com.adc.adcampaign.finance.invoicing.entity.InvoiceLineItem;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Builds a real, parseable PDF from an invoice snapshot — plain text layout,
 * no table graphics. Not stored anywhere; regenerated on demand from the
 * frozen Invoice/InvoiceLineItem rows, which produce identical content every
 * time.
 */
@Component
public class InvoicePdfGenerator {

    private static final float MARGIN_X = 50;
    private static final float START_Y = 740;
    private static final float LINE_HEIGHT = 18;

    public byte[] generate(Invoice invoice, Advertiser advertiser, List<InvoiceLineItem> lineItems) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                PDType1Font body = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

                content.beginText();
                content.setFont(bold, 16);
                content.newLineAtOffset(MARGIN_X, START_Y);
                content.showText("Invoice #" + invoice.getId());
                content.setFont(body, 11);
                content.newLineAtOffset(0, -LINE_HEIGHT * 1.5f);
                content.showText("Bill to: " + advertiser.getName());
                content.newLineAtOffset(0, -LINE_HEIGHT);
                content.showText("Period: " + invoice.getPeriodStart() + " to " + invoice.getPeriodEnd());
                content.newLineAtOffset(0, -LINE_HEIGHT);
                content.showText("Status: " + invoice.getStatus());
                content.newLineAtOffset(0, -LINE_HEIGHT * 2);

                content.setFont(bold, 11);
                content.showText("Campaign");
                content.newLineAtOffset(300, 0);
                content.showText("Amount (" + invoice.getCurrency() + ")");
                content.newLineAtOffset(-300, -LINE_HEIGHT);
                content.setFont(body, 11);

                for (InvoiceLineItem item : lineItems) {
                    content.showText(item.getDescription());
                    content.newLineAtOffset(300, 0);
                    content.showText(item.getAmount().toPlainString());
                    content.newLineAtOffset(-300, -LINE_HEIGHT);
                }

                content.newLineAtOffset(0, -LINE_HEIGHT / 2);
                content.setFont(bold, 12);
                content.showText("Total: " + invoice.getTotalAmount().toPlainString() + " " + invoice.getCurrency());
                content.endText();
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to generate invoice PDF", e);
        }
    }
}
