package com.adc.adcampaign.campaign.controller;

import com.adc.adcampaign.campaign.entity.Budget;
import com.adc.adcampaign.campaign.service.BudgetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    @Autowired
    private BudgetService budgetService;

    @PostMapping
    public ResponseEntity<Budget> createOrUpdate(@RequestBody Budget budget) {
        return ResponseEntity.ok(budgetService.createOrUpdateBudget(budget));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Budget> getById(@PathVariable Long id) {
        Optional<Budget> budget = budgetService.getBudgetById(id);
        return budget.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/campaign/{campaignId}")
    public ResponseEntity<Budget> getByCampaign(@PathVariable Long campaignId) {
        Optional<Budget> budget = budgetService.getBudgetByCampaignId(campaignId);
        return budget.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        budgetService.deleteBudget(id);
        return ResponseEntity.noContent().build();
    }
}
