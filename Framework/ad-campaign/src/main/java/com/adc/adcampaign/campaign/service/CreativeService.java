package com.adc.adcampaign.campaign.service;

import com.adc.adcampaign.campaign.entity.Creative;
import com.adc.adcampaign.campaign.repository.CreativeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CreativeService {

    @Autowired
    private CreativeRepository creativeRepository;

    public Creative createCreative(Creative creative) {
        return creativeRepository.save(creative);
    }

    public List<Creative> getAllCreatives() {
        return creativeRepository.findAll();
    }

    public Optional<Creative> getCreativeById(Long id) {
        return creativeRepository.findById(id);
    }

    public List<Creative> getCreativesByCampaign(Long campaignId) {
        return creativeRepository.findByCampaignId(campaignId);
    }

    public Creative updateCreative(Creative creative) {
        return creativeRepository.save(creative);
    }

    public void deleteCreative(Long id) {
        creativeRepository.deleteById(id);
    }
}
