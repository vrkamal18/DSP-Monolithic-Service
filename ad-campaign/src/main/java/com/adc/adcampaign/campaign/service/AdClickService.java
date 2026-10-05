package com.adc.adcampaign.campaign.service;

import com.adc.adcampaign.campaign.entity.AdClick;
import com.adc.adcampaign.campaign.repository.AdClickRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AdClickService {

    @Autowired
    private AdClickRepository adClickRepository;

    public AdClick logClick(AdClick click) {
        return adClickRepository.save(click);
    }
}

