package com.adc.adcampaign.campaign.service;

import com.adc.adcampaign.campaign.entity.AdImpression;
import com.adc.adcampaign.campaign.repository.AdImpressionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Persists the measurement/audit record for a served impression. Budget and
 * pacing accounting is a finance concern and is handled by
 * {@code com.adc.adcampaign.bidding.PacingService} using the actual winning
 * bid price, not a mock cost.
 */
@Service
public class AdImpressionService {

    @Autowired
    private AdImpressionRepository adImpressionRepository;

    public AdImpression logImpression(AdImpression impression) {
        return adImpressionRepository.save(impression);
    }

}

