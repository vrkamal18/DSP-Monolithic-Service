package com.adc.adcampaign.campaign.service;

import com.adc.adcampaign.campaign.entity.Advertiser;
import com.adc.adcampaign.campaign.repository.AdvertiserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AdvertiserService {

    @Autowired
    private AdvertiserRepository advertiserRepository;

    public Advertiser createAdvertiser(Advertiser advertiser) {
        return advertiserRepository.save(advertiser);
    }

    public List<Advertiser> getAllAdvertisers() {
        return advertiserRepository.findAll();
    }

    public Optional<Advertiser> getAdvertiserById(Long id) {
        return advertiserRepository.findById(id);
    }

    public Advertiser updateAdvertiser(Advertiser advertiser) {
        return advertiserRepository.save(advertiser);
    }

    public void deleteAdvertiser(Long id) {
        advertiserRepository.deleteById(id);
    }
}
