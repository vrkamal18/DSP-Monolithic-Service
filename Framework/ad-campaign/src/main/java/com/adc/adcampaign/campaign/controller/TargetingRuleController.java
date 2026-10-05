package com.adc.adcampaign.campaign.controller;

import com.adc.adcampaign.campaign.dto.TargetingRuleRequest;
import com.adc.adcampaign.campaign.dto.TargetingRuleResponse;
import com.adc.adcampaign.campaign.service.TargetingRuleService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/targeting-rules")
public class TargetingRuleController {

    private final TargetingRuleService targetingRuleService;

    public TargetingRuleController(TargetingRuleService targetingRuleService) {
        this.targetingRuleService = targetingRuleService;
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'CAMPAIGN_MANAGER')")
    @PostMapping
    public ResponseEntity<TargetingRuleResponse> createRule(@Valid @RequestBody TargetingRuleRequest request) {
        return ResponseEntity.ok(TargetingRuleResponse.from(targetingRuleService.createTargetingRule(request)));
    }

    @GetMapping
    public ResponseEntity<List<TargetingRuleResponse>> getAllRules() {
        return ResponseEntity.ok(targetingRuleService.getAllTargetingRules().stream()
                .map(TargetingRuleResponse::from)
                .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TargetingRuleResponse> getRuleById(@PathVariable Long id) {
        return ResponseEntity.ok(TargetingRuleResponse.from(targetingRuleService.getTargetingRuleById(id)));
    }

    @GetMapping("/campaign/{campaignId}")
    public ResponseEntity<List<TargetingRuleResponse>> getRulesByCampaign(@PathVariable Long campaignId) {
        return ResponseEntity.ok(targetingRuleService.getRulesByCampaign(campaignId).stream()
                .map(TargetingRuleResponse::from)
                .toList());
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'CAMPAIGN_MANAGER')")
    @PutMapping("/{id}")
    public ResponseEntity<TargetingRuleResponse> updateRule(@PathVariable Long id,
                                                             @Valid @RequestBody TargetingRuleRequest request) {
        return ResponseEntity.ok(TargetingRuleResponse.from(targetingRuleService.updateTargetingRule(id, request)));
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'CAMPAIGN_MANAGER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
        targetingRuleService.deleteTargetingRule(id);
        return ResponseEntity.noContent().build();
    }
}
