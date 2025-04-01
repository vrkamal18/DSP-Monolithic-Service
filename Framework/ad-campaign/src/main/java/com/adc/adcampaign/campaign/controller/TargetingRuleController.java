package com.adc.adcampaign.campaign.controller;

import com.adc.adcampaign.campaign.entity.TargetingRule;
import com.adc.adcampaign.campaign.service.TargetingRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/targeting-rules")
public class TargetingRuleController {

    @Autowired
    private TargetingRuleService targetingRuleService;

    @PostMapping
    public ResponseEntity<TargetingRule> createRule(@RequestBody TargetingRule rule) {
        return ResponseEntity.ok(targetingRuleService.createTargetingRule(rule));
    }

    @GetMapping
    public ResponseEntity<List<TargetingRule>> getAllRules() {
        return ResponseEntity.ok(targetingRuleService.getAllTargetingRules());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TargetingRule> getRuleById(@PathVariable Long id) {
        return targetingRuleService.getTargetingRuleById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/campaign/{campaignId}")
    public ResponseEntity<List<TargetingRule>> getRulesByCampaign(@PathVariable Long campaignId) {
        return ResponseEntity.ok(targetingRuleService.getRulesByCampaign(campaignId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TargetingRule> updateRule(@PathVariable Long id, @RequestBody TargetingRule rule) {
        rule.setId(id);
        return ResponseEntity.ok(targetingRuleService.updateTargetingRule(rule));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
        targetingRuleService.deleteTargetingRule(id);
        return ResponseEntity.noContent().build();
    }
}
