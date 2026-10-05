package com.adc.adcampaign.finance.invoicing;

import com.adc.adcampaign.campaign.entity.Advertiser;
import com.adc.adcampaign.campaign.entity.Campaign;
import com.adc.adcampaign.campaign.repository.AdvertiserRepository;
import com.adc.adcampaign.campaign.repository.CampaignRepository;
import com.adc.adcampaign.finance.billing.entity.LedgerEntry;
import com.adc.adcampaign.finance.billing.entity.LedgerEntryType;
import com.adc.adcampaign.finance.billing.repository.LedgerEntryRepository;
import com.adc.adcampaign.finance.invoicing.entity.Invoice;
import com.adc.adcampaign.finance.invoicing.entity.InvoiceLineItem;
import com.adc.adcampaign.finance.invoicing.entity.InvoiceStatus;
import com.adc.adcampaign.finance.invoicing.repository.InvoiceLineItemRepository;
import com.adc.adcampaign.finance.invoicing.repository.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InvoiceServiceTest {

    private static final Long TENANT_ID = 1L;
    private static final Long ADVERTISER_ID = 5L;
    private static final LocalDate PERIOD_START = LocalDate.of(2026, 1, 1);
    private static final LocalDate PERIOD_END = LocalDate.of(2026, 1, 31);

    @Mock private InvoiceRepository invoiceRepository;
    @Mock private InvoiceLineItemRepository invoiceLineItemRepository;
    @Mock private LedgerEntryRepository ledgerEntryRepository;
    @Mock private CampaignRepository campaignRepository;
    @Mock private AdvertiserRepository advertiserRepository;
    @Mock private InvoicePdfGenerator pdfGenerator;
    @Mock private InvoiceEmailAdapter emailAdapter;

    private InvoiceService invoiceService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        invoiceService = new InvoiceService(invoiceRepository, invoiceLineItemRepository, ledgerEntryRepository,
                campaignRepository, advertiserRepository, pdfGenerator, emailAdapter);

        when(advertiserRepository.findByIdAndTenantId(ADVERTISER_ID, TENANT_ID))
                .thenReturn(Optional.of(Advertiser.builder().id(ADVERTISER_ID).name("Adv").email("adv@example.com").build()));
        when(invoiceRepository.save(any())).thenAnswer(inv -> {
            Invoice invoice = inv.getArgument(0);
            if (invoice.getId() == null) invoice.setId(100L);
            return invoice;
        });
    }

    private Campaign campaign(long id, String name) {
        return Campaign.builder().id(id).name(name)
                .tenant(com.adc.adcampaign.identity.entity.Tenant.builder().id(TENANT_ID).build())
                .build();
    }

    @Test
    void generateAggregatesLedgerEntriesIntoPerCampaignLineItemsAndTotal() {
        when(invoiceRepository.findByTenantIdAndAdvertiserIdAndPeriodStartAndPeriodEnd(
                TENANT_ID, ADVERTISER_ID, PERIOD_START, PERIOD_END)).thenReturn(Optional.empty());
        when(campaignRepository.findByAdvertiserId(ADVERTISER_ID)).thenReturn(List.of(campaign(10L, "Campaign A")));
        when(campaignRepository.findAllById(List.of(10L))).thenReturn(List.of(campaign(10L, "Campaign A")));
        when(ledgerEntryRepository.findByTenantIdAndCampaignIdInAndCreatedAtBetween(
                org.mockito.ArgumentMatchers.eq(TENANT_ID), org.mockito.ArgumentMatchers.eq(List.of(10L)), any(), any()))
                .thenReturn(List.of(
                        LedgerEntry.builder().campaignId(10L).entryType(LedgerEntryType.CHARGE).totalAmount(BigDecimal.valueOf(2.20)).build(),
                        LedgerEntry.builder().campaignId(10L).entryType(LedgerEntryType.CHARGE).totalAmount(BigDecimal.valueOf(3.30)).build()));

        Invoice invoice = invoiceService.generate(TENANT_ID, ADVERTISER_ID, PERIOD_START, PERIOD_END);

        assertThat(invoice.getTotalAmount()).isEqualByComparingTo("5.50");
        verify(invoiceLineItemRepository).save(org.mockito.ArgumentMatchers.argThat(item ->
                item.getCampaignId().equals(10L) && item.getAmount().compareTo(BigDecimal.valueOf(5.50)) == 0
                        && "Campaign A".equals(item.getDescription())));
    }

    @Test
    void generateRejectsADuplicatePeriod() {
        when(invoiceRepository.findByTenantIdAndAdvertiserIdAndPeriodStartAndPeriodEnd(
                TENANT_ID, ADVERTISER_ID, PERIOD_START, PERIOD_END))
                .thenReturn(Optional.of(Invoice.builder().id(1L).build()));

        assertThatThrownBy(() -> invoiceService.generate(TENANT_ID, ADVERTISER_ID, PERIOD_START, PERIOD_END))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void generateProducesAZeroAmountInvoiceWhenThereIsNoLedgerActivity() {
        when(invoiceRepository.findByTenantIdAndAdvertiserIdAndPeriodStartAndPeriodEnd(
                TENANT_ID, ADVERTISER_ID, PERIOD_START, PERIOD_END)).thenReturn(Optional.empty());
        when(campaignRepository.findByAdvertiserId(ADVERTISER_ID)).thenReturn(List.of());

        Invoice invoice = invoiceService.generate(TENANT_ID, ADVERTISER_ID, PERIOD_START, PERIOD_END);

        assertThat(invoice.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void issueMovesDraftToIssued() {
        when(invoiceRepository.findByIdAndTenantId(1L, TENANT_ID))
                .thenReturn(Optional.of(Invoice.builder().id(1L).status(InvoiceStatus.DRAFT).build()));

        Invoice result = invoiceService.issue(TENANT_ID, 1L);

        assertThat(result.getStatus()).isEqualTo(InvoiceStatus.ISSUED);
        assertThat(result.getIssuedAt()).isNotNull();
    }

    @Test
    void issueFailsWhenNotDraft() {
        when(invoiceRepository.findByIdAndTenantId(1L, TENANT_ID))
                .thenReturn(Optional.of(Invoice.builder().id(1L).status(InvoiceStatus.SENT).build()));

        assertThatThrownBy(() -> invoiceService.issue(TENANT_ID, 1L)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void sendGeneratesPdfAndEmailsTheAdvertiser() {
        Invoice invoice = Invoice.builder().id(1L).advertiserId(ADVERTISER_ID).status(InvoiceStatus.ISSUED).build();
        when(invoiceRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(invoice));
        when(invoiceLineItemRepository.findByInvoiceId(1L)).thenReturn(List.of());
        when(pdfGenerator.generate(any(), any(), any())).thenReturn(new byte[]{1, 2, 3});

        Invoice result = invoiceService.send(TENANT_ID, 1L);

        assertThat(result.getStatus()).isEqualTo(InvoiceStatus.SENT);
        verify(emailAdapter).send(anyString(), anyString(), any());
    }

    @Test
    void voidFailsFromPaid() {
        when(invoiceRepository.findByIdAndTenantId(1L, TENANT_ID))
                .thenReturn(Optional.of(Invoice.builder().id(1L).status(InvoiceStatus.PAID).build()));

        assertThatThrownBy(() -> invoiceService.voidInvoice(TENANT_ID, 1L)).isInstanceOf(IllegalStateException.class);
    }
}
