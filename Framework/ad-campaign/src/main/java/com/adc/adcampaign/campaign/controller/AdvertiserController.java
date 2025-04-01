package com.adc.adcampaign.campaign.controller;

import com.adc.adcampaign.campaign.entity.Advertiser;
import com.adc.adcampaign.campaign.service.AdvertiserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/advertisers")
public class AdvertiserController {

    @Autowired
    private AdvertiserService advertiserService;

    @PostMapping
    public ResponseEntity<Advertiser> createAdvertiser(@RequestBody Advertiser advertiser) {
        return ResponseEntity.ok(advertiserService.createAdvertiser(advertiser));
    }

    @GetMapping
    public ResponseEntity<List<Advertiser>> getAllAdvertisers() {
        return ResponseEntity.ok(advertiserService.getAllAdvertisers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Advertiser> getAdvertiserById(@PathVariable Long id) {
        return advertiserService.getAdvertiserById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Advertiser> updateAdvertiser(@PathVariable Long id, @RequestBody Advertiser advertiser) {
        advertiser.setId(id);
        return ResponseEntity.ok(advertiserService.updateAdvertiser(advertiser));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAdvertiser(@PathVariable Long id) {
        advertiserService.deleteAdvertiser(id);
        return ResponseEntity.noContent().build();
    }
}
