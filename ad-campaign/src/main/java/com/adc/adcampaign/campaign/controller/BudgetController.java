package com.adc.adcampaign.campaign.controller;

import com.adc.adcampaign.campaign.dto.BudgetRequest;
import com.adc.adcampaign.campaign.dto.BudgetResponse;
import com.adc.adcampaign.campaign.service.BudgetService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/budgets")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'CAMPAIGN_MANAGER', 'FINANCE_USER')")
    @PostMapping
    public ResponseEntity<BudgetResponse> createOrUpdate(@Valid @RequestBody BudgetRequest request) {
        return ResponseEntity.ok(BudgetResponse.from(budgetService.createOrUpdateBudget(request)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BudgetResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(BudgetResponse.from(budgetService.getBudgetById(id)));
    }

    @GetMapping("/campaign/{campaignId}")
    public ResponseEntity<BudgetResponse> getByCampaign(@PathVariable Long campaignId) {
        return ResponseEntity.ok(BudgetResponse.from(budgetService.getBudgetByCampaignId(campaignId)));
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'FINANCE_USER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        budgetService.deleteBudget(id);
        return ResponseEntity.noContent().build();
    }
}
