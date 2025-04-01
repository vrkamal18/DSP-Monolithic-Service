package com.adc.adcampaign.campaign.controller;

import com.adc.adcampaign.campaign.entity.Creative;
import com.adc.adcampaign.campaign.service.CreativeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/creatives")
public class CreativeController {

    @Autowired
    private CreativeService creativeService;

    @PostMapping
    public ResponseEntity<Creative> createCreative(@RequestBody Creative creative) {
        return ResponseEntity.ok(creativeService.createCreative(creative));
    }

    @GetMapping
    public ResponseEntity<List<Creative>> getAllCreatives() {
        return ResponseEntity.ok(creativeService.getAllCreatives());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Creative> getCreativeById(@PathVariable Long id) {
        return creativeService.getCreativeById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/campaign/{campaignId}")
    public ResponseEntity<List<Creative>> getCreativesByCampaign(@PathVariable Long campaignId) {
        return ResponseEntity.ok(creativeService.getCreativesByCampaign(campaignId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Creative> updateCreative(@PathVariable Long id, @RequestBody Creative creative) {
        creative.setId(id);
        return ResponseEntity.ok(creativeService.updateCreative(creative));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCreative(@PathVariable Long id) {
        creativeService.deleteCreative(id);
        return ResponseEntity.noContent().build();
    }
}
