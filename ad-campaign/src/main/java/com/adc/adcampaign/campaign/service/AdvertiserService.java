package com.adc.adcampaign.campaign.service;

import com.adc.adcampaign.campaign.dto.AdvertiserRequest;
import com.adc.adcampaign.campaign.entity.Advertiser;
import com.adc.adcampaign.campaign.repository.AdvertiserRepository;
import com.adc.adcampaign.foundation.error.NotFoundException;
import com.adc.adcampaign.foundation.security.TenantContext;
import com.adc.adcampaign.identity.entity.Tenant;
import com.adc.adcampaign.identity.repository.TenantRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdvertiserService {

    private final AdvertiserRepository advertiserRepository;
    private final TenantRepository tenantRepository;

    public AdvertiserService(AdvertiserRepository advertiserRepository, TenantRepository tenantRepository) {
        this.advertiserRepository = advertiserRepository;
        this.tenantRepository = tenantRepository;
    }

    public Advertiser createAdvertiser(AdvertiserRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        Tenant tenant = tenantRepository.getReferenceById(tenantId);
        Advertiser advertiser = Advertiser.builder()
                .tenant(tenant)
                .name(request.getName())
                .email(request.getEmail())
                .company(request.getCompany())
                .build();
        return advertiserRepository.save(advertiser);
    }

    public List<Advertiser> getAllAdvertisers() {
        return advertiserRepository.findByTenantId(TenantContext.requireTenantId());
    }

    public Advertiser getAdvertiserById(Long id) {
        return advertiserRepository.findByIdAndTenantId(id, TenantContext.requireTenantId())
                .orElseThrow(() -> new NotFoundException("Advertiser not found: " + id));
    }

    public Advertiser updateAdvertiser(Long id, AdvertiserRequest request) {
        Advertiser existing = getAdvertiserById(id);
        existing.setName(request.getName());
        existing.setEmail(request.getEmail());
        existing.setCompany(request.getCompany());
        return advertiserRepository.save(existing);
    }

    public void deleteAdvertiser(Long id) {
        advertiserRepository.delete(getAdvertiserById(id));
    }
}
