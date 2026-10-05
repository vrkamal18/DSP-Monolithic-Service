package com.adc.adcampaign.finance.payments.controller;

import com.adc.adcampaign.finance.payments.PaymentMethodService;
import com.adc.adcampaign.finance.payments.dto.PaymentMethodRequest;
import com.adc.adcampaign.finance.payments.dto.PaymentMethodResponse;
import com.adc.adcampaign.foundation.security.TenantContext;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payment-methods")
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;

    public PaymentMethodController(PaymentMethodService paymentMethodService) {
        this.paymentMethodService = paymentMethodService;
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'FINANCE_USER')")
    @PostMapping
    public ResponseEntity<PaymentMethodResponse> register(@Valid @RequestBody PaymentMethodRequest request) {
        var method = paymentMethodService.register(TenantContext.requireTenantId(), request);
        return ResponseEntity.ok(PaymentMethodResponse.from(method));
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'FINANCE_USER', 'ANALYST')")
    @GetMapping
    public ResponseEntity<List<PaymentMethodResponse>> list(@RequestParam Long advertiserId) {
        return ResponseEntity.ok(paymentMethodService.listForAdvertiser(TenantContext.requireTenantId(), advertiserId)
                .stream()
                .map(PaymentMethodResponse::from)
                .toList());
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'FINANCE_USER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        paymentMethodService.delete(TenantContext.requireTenantId(), id);
        return ResponseEntity.noContent().build();
    }
}
