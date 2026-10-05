package com.adc.adcampaign.finance.invoicing;

import com.adc.adcampaign.campaign.entity.Advertiser;
import com.adc.adcampaign.campaign.entity.Campaign;
import com.adc.adcampaign.campaign.repository.AdvertiserRepository;
import com.adc.adcampaign.campaign.repository.CampaignRepository;
import com.adc.adcampaign.finance.billing.entity.LedgerEntry;
import com.adc.adcampaign.finance.billing.repository.LedgerEntryRepository;
import com.adc.adcampaign.finance.invoicing.entity.Invoice;
import com.adc.adcampaign.finance.invoicing.entity.InvoiceLineItem;
import com.adc.adcampaign.finance.invoicing.entity.InvoiceStatus;
import com.adc.adcampaign.finance.invoicing.repository.InvoiceLineItemRepository;
import com.adc.adcampaign.finance.invoicing.repository.InvoiceRepository;
import com.adc.adcampaign.foundation.error.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineItemRepository invoiceLineItemRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final CampaignRepository campaignRepository;
    private final AdvertiserRepository advertiserRepository;
    private final InvoicePdfGenerator pdfGenerator;
    private final InvoiceEmailAdapter emailAdapter;

    public InvoiceService(InvoiceRepository invoiceRepository,
                           InvoiceLineItemRepository invoiceLineItemRepository,
                           LedgerEntryRepository ledgerEntryRepository,
                           CampaignRepository campaignRepository,
                           AdvertiserRepository advertiserRepository,
                           InvoicePdfGenerator pdfGenerator,
                           InvoiceEmailAdapter emailAdapter) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceLineItemRepository = invoiceLineItemRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.campaignRepository = campaignRepository;
        this.advertiserRepository = advertiserRepository;
        this.pdfGenerator = pdfGenerator;
        this.emailAdapter = emailAdapter;
    }

    @Transactional
    public Invoice generate(Long tenantId, Long advertiserId, LocalDate periodStart, LocalDate periodEnd) {
        Advertiser advertiser = advertiserRepository.findByIdAndTenantId(advertiserId, tenantId)
                .orElseThrow(() -> new NotFoundException("Advertiser not found: " + advertiserId));

        if (invoiceRepository.findByTenantIdAndAdvertiserIdAndPeriodStartAndPeriodEnd(
                tenantId, advertiserId, periodStart, periodEnd).isPresent()) {
            throw new IllegalStateException(
                    "An invoice already exists for advertiser " + advertiserId + " for this period");
        }

        List<Long> campaignIds = campaignRepository.findByAdvertiserId(advertiserId).stream()
                .filter(c -> c.getTenant().getId().equals(tenantId))
                .map(Campaign::getId)
                .toList();

        Map<Long, String> campaignNames = campaignRepository.findAllById(campaignIds).stream()
                .collect(Collectors.toMap(Campaign::getId, Campaign::getName));

        List<LedgerEntry> entries = campaignIds.isEmpty() ? List.of()
                : ledgerEntryRepository.findByTenantIdAndCampaignIdInAndCreatedAtBetween(
                        tenantId, campaignIds, periodStart.atStartOfDay(), LocalDateTime.of(periodEnd, LocalTime.MAX));

        Map<Long, BigDecimal> totalsByCampaign = entries.stream()
                .filter(e -> e.getCampaignId() != null)
                .collect(Collectors.groupingBy(LedgerEntry::getCampaignId,
                        Collectors.reducing(BigDecimal.ZERO, LedgerEntry::getTotalAmount, BigDecimal::add)));

        BigDecimal total = totalsByCampaign.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        Invoice invoice = invoiceRepository.save(Invoice.builder()
                .tenantId(tenantId)
                .advertiserId(advertiserId)
                .status(InvoiceStatus.DRAFT)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .totalAmount(total)
                .currency("USD")
                .build());

        totalsByCampaign.forEach((campaignId, amount) -> invoiceLineItemRepository.save(InvoiceLineItem.builder()
                .invoiceId(invoice.getId())
                .campaignId(campaignId)
                .description(campaignNames.getOrDefault(campaignId, "Campaign " + campaignId))
                .amount(amount)
                .build()));

        return invoice;
    }

    public Invoice getById(Long tenantId, Long id) {
        return invoiceRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Invoice not found: " + id));
    }

    public List<Invoice> listForTenant(Long tenantId) {
        return invoiceRepository.findByTenantId(tenantId);
    }

    public List<InvoiceLineItem> getLineItems(Long invoiceId) {
        return invoiceLineItemRepository.findByInvoiceId(invoiceId).stream()
                .sorted(Comparator.comparing(InvoiceLineItem::getId))
                .toList();
    }

    @Transactional
    public Invoice issue(Long tenantId, Long id) {
        Invoice invoice = getById(tenantId, id);
        requireTransitionFrom(invoice.getStatus(), EnumSet.of(InvoiceStatus.DRAFT));
        invoice.setStatus(InvoiceStatus.ISSUED);
        invoice.setIssuedAt(LocalDateTime.now());
        return invoiceRepository.save(invoice);
    }

    @Transactional
    public Invoice send(Long tenantId, Long id) {
        Invoice invoice = getById(tenantId, id);
        requireTransitionFrom(invoice.getStatus(), EnumSet.of(InvoiceStatus.ISSUED));

        Advertiser advertiser = advertiserRepository.findByIdAndTenantId(invoice.getAdvertiserId(), tenantId)
                .orElseThrow(() -> new NotFoundException("Advertiser not found: " + invoice.getAdvertiserId()));
        byte[] pdf = pdfGenerator.generate(invoice, advertiser, getLineItems(invoice.getId()));
        emailAdapter.send(advertiser.getEmail(), "Invoice #" + invoice.getId(), pdf);

        invoice.setStatus(InvoiceStatus.SENT);
        invoice.setSentAt(LocalDateTime.now());
        return invoiceRepository.save(invoice);
    }

    @Transactional
    public Invoice markPaid(Long tenantId, Long id) {
        Invoice invoice = getById(tenantId, id);
        requireTransitionFrom(invoice.getStatus(), EnumSet.of(InvoiceStatus.SENT));
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(LocalDateTime.now());
        return invoiceRepository.save(invoice);
    }

    @Transactional
    public Invoice voidInvoice(Long tenantId, Long id) {
        Invoice invoice = getById(tenantId, id);
        requireTransitionFrom(invoice.getStatus(), EnumSet.of(InvoiceStatus.DRAFT, InvoiceStatus.ISSUED));
        invoice.setStatus(InvoiceStatus.VOID);
        invoice.setVoidedAt(LocalDateTime.now());
        return invoiceRepository.save(invoice);
    }

    public byte[] getDocument(Long tenantId, Long id) {
        Invoice invoice = getById(tenantId, id);
        Advertiser advertiser = advertiserRepository.findByIdAndTenantId(invoice.getAdvertiserId(), tenantId)
                .orElseThrow(() -> new NotFoundException("Advertiser not found: " + invoice.getAdvertiserId()));
        return pdfGenerator.generate(invoice, advertiser, getLineItems(invoice.getId()));
    }

    private void requireTransitionFrom(InvoiceStatus current, Set<InvoiceStatus> allowed) {
        if (!allowed.contains(current)) {
            throw new IllegalStateException("Cannot transition invoice from " + current);
        }
    }
}
