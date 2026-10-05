package com.adc.adcampaign.finance.payments.controller;

import com.adc.adcampaign.finance.payments.AccountService;
import com.adc.adcampaign.finance.payments.dto.AccountBalanceResponse;
import com.adc.adcampaign.finance.payments.dto.RefundRequest;
import com.adc.adcampaign.finance.payments.dto.TopUpRequest;
import com.adc.adcampaign.finance.payments.dto.TransactionResponse;
import com.adc.adcampaign.finance.payments.entity.Account;
import com.adc.adcampaign.foundation.security.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@Validated
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'FINANCE_USER', 'ANALYST')")
    @GetMapping("/{advertiserId}/balance")
    public ResponseEntity<AccountBalanceResponse> getBalance(@PathVariable Long advertiserId) {
        Account account = accountService.getBalance(TenantContext.requireTenantId(), advertiserId);
        return ResponseEntity.ok(AccountBalanceResponse.builder()
                .advertiserId(advertiserId)
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .build());
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'FINANCE_USER', 'ANALYST')")
    @GetMapping("/{advertiserId}/transactions")
    public ResponseEntity<List<TransactionResponse>> getTransactions(@PathVariable Long advertiserId) {
        return ResponseEntity.ok(accountService.listTransactions(TenantContext.requireTenantId(), advertiserId)
                .stream()
                .map(TransactionResponse::from)
                .toList());
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'FINANCE_USER')")
    @PostMapping("/{advertiserId}/topup")
    public ResponseEntity<TransactionResponse> topUp(@PathVariable Long advertiserId,
                                                       @Valid @RequestBody TopUpRequest request,
                                                       @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey) {
        var transaction = accountService.topUp(TenantContext.requireTenantId(), advertiserId,
                request.getPaymentMethodId(), request.getAmount(), idempotencyKey);
        return ResponseEntity.ok(TransactionResponse.from(transaction));
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'FINANCE_USER')")
    @PostMapping("/{advertiserId}/refund")
    public ResponseEntity<TransactionResponse> refund(@PathVariable Long advertiserId,
                                                        @Valid @RequestBody RefundRequest request,
                                                        @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey) {
        var transaction = accountService.refund(TenantContext.requireTenantId(), advertiserId,
                request.getAmount(), idempotencyKey);
        return ResponseEntity.ok(TransactionResponse.from(transaction));
    }
}
