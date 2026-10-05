package com.adc.adcampaign.finance.invoicing.controller;

import com.adc.adcampaign.finance.invoicing.InvoiceService;
import com.adc.adcampaign.finance.invoicing.dto.GenerateInvoiceRequest;
import com.adc.adcampaign.finance.invoicing.dto.InvoiceResponse;
import com.adc.adcampaign.finance.invoicing.entity.Invoice;
import com.adc.adcampaign.foundation.security.TenantContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'FINANCE_USER')")
    @PostMapping
    public ResponseEntity<InvoiceResponse> generate(@Valid @RequestBody GenerateInvoiceRequest request) {
        Invoice invoice = invoiceService.generate(TenantContext.requireTenantId(), request.getAdvertiserId(),
                request.getPeriodStart(), request.getPeriodEnd());
        return ResponseEntity.ok(toResponse(invoice));
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'FINANCE_USER', 'ANALYST')")
    @GetMapping
    public ResponseEntity<List<InvoiceResponse>> list() {
        Long tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(invoiceService.listForTenant(tenantId).stream()
                .map(this::toResponse)
                .toList());
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'FINANCE_USER', 'ANALYST')")
    @GetMapping("/{id}")
    public ResponseEntity<InvoiceResponse> getById(@PathVariable Long id) {
        Invoice invoice = invoiceService.getById(TenantContext.requireTenantId(), id);
        return ResponseEntity.ok(toResponse(invoice));
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'FINANCE_USER', 'ANALYST')")
    @GetMapping("/{id}/document")
    public ResponseEntity<byte[]> getDocument(@PathVariable Long id) {
        byte[] pdf = invoiceService.getDocument(TenantContext.requireTenantId(), id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"invoice-" + id + ".pdf\"")
                .body(pdf);
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'FINANCE_USER')")
    @PostMapping("/{id}/issue")
    public ResponseEntity<InvoiceResponse> issue(@PathVariable Long id) {
        return ResponseEntity.ok(toResponse(invoiceService.issue(TenantContext.requireTenantId(), id)));
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'FINANCE_USER')")
    @PostMapping("/{id}/send")
    public ResponseEntity<InvoiceResponse> send(@PathVariable Long id) {
        return ResponseEntity.ok(toResponse(invoiceService.send(TenantContext.requireTenantId(), id)));
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'FINANCE_USER')")
    @PostMapping("/{id}/mark-paid")
    public ResponseEntity<InvoiceResponse> markPaid(@PathVariable Long id) {
        return ResponseEntity.ok(toResponse(invoiceService.markPaid(TenantContext.requireTenantId(), id)));
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'FINANCE_USER')")
    @PostMapping("/{id}/void")
    public ResponseEntity<InvoiceResponse> voidInvoice(@PathVariable Long id) {
        return ResponseEntity.ok(toResponse(invoiceService.voidInvoice(TenantContext.requireTenantId(), id)));
    }

    private InvoiceResponse toResponse(Invoice invoice) {
        return InvoiceResponse.from(invoice, invoiceService.getLineItems(invoice.getId()));
    }
}
