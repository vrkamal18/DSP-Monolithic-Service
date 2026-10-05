package com.adc.adcampaign.campaign.service;

import com.adc.adcampaign.campaign.dto.CreativeRequest;
import com.adc.adcampaign.campaign.entity.Campaign;
import com.adc.adcampaign.campaign.entity.Creative;
import com.adc.adcampaign.campaign.entity.CreativeStatus;
import com.adc.adcampaign.campaign.repository.CampaignRepository;
import com.adc.adcampaign.campaign.repository.CreativeRepository;
import com.adc.adcampaign.foundation.error.NotFoundException;
import com.adc.adcampaign.foundation.security.TenantContext;
import com.adc.adcampaign.identity.entity.Tenant;
import com.adc.adcampaign.identity.repository.TenantRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
public class CreativeService {

    private final CreativeRepository creativeRepository;
    private final CampaignRepository campaignRepository;
    private final TenantRepository tenantRepository;

    public CreativeService(CreativeRepository creativeRepository,
                            CampaignRepository campaignRepository,
                            TenantRepository tenantRepository) {
        this.creativeRepository = creativeRepository;
        this.campaignRepository = campaignRepository;
        this.tenantRepository = tenantRepository;
    }

    public Creative createCreative(CreativeRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        Tenant tenant = tenantRepository.getReferenceById(tenantId);
        Campaign campaign = resolveCampaign(request.getCampaignId(), tenantId);

        Creative creative = Creative.builder()
                .tenant(tenant)
                .campaign(campaign)
                .name(request.getName())
                .type(request.getType())
                .url(request.getUrl())
                .width(request.getWidth())
                .height(request.getHeight())
                .status(CreativeStatus.DRAFT)
                .build();
        return creativeRepository.save(creative);
    }

    public List<Creative> getAllCreatives() {
        return creativeRepository.findByTenantId(TenantContext.requireTenantId());
    }

    public Creative getCreativeById(Long id) {
        return creativeRepository.findByIdAndTenantId(id, TenantContext.requireTenantId())
                .orElseThrow(() -> new NotFoundException("Creative not found: " + id));
    }

    public List<Creative> getCreativesByCampaign(Long campaignId) {
        return creativeRepository.findByCampaignIdAndTenantId(campaignId, TenantContext.requireTenantId());
    }

    public Creative updateCreative(Long id, CreativeRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        Creative existing = getCreativeById(id);
        existing.setCampaign(resolveCampaign(request.getCampaignId(), tenantId));
        existing.setName(request.getName());
        existing.setType(request.getType());
        existing.setUrl(request.getUrl());
        existing.setWidth(request.getWidth());
        existing.setHeight(request.getHeight());
        return creativeRepository.save(existing);
    }

    public void deleteCreative(Long id) {
        creativeRepository.delete(getCreativeById(id));
    }

    /** DRAFT or REJECTED -> PENDING_REVIEW. */
    public Creative submit(Long id) {
        Creative creative = getCreativeById(id);
        requireTransitionFrom(creative.getStatus(), EnumSet.of(CreativeStatus.DRAFT, CreativeStatus.REJECTED));
        creative.setStatus(CreativeStatus.PENDING_REVIEW);
        creative.setRejectionReason(null);
        return creativeRepository.save(creative);
    }

    /** PENDING_REVIEW -> APPROVED. */
    public Creative approve(Long id) {
        Creative creative = getCreativeById(id);
        requireTransitionFrom(creative.getStatus(), EnumSet.of(CreativeStatus.PENDING_REVIEW));
        creative.setStatus(CreativeStatus.APPROVED);
        creative.setRejectionReason(null);
        creative.setReviewedByUserId(TenantContext.requireUserId());
        creative.setReviewedAt(LocalDateTime.now());
        return creativeRepository.save(creative);
    }

    /** PENDING_REVIEW -> REJECTED, with a required reason. */
    public Creative reject(Long id, String reason) {
        Creative creative = getCreativeById(id);
        requireTransitionFrom(creative.getStatus(), EnumSet.of(CreativeStatus.PENDING_REVIEW));
        creative.setStatus(CreativeStatus.REJECTED);
        creative.setRejectionReason(reason);
        creative.setReviewedByUserId(TenantContext.requireUserId());
        creative.setReviewedAt(LocalDateTime.now());
        return creativeRepository.save(creative);
    }

    /** Any non-ARCHIVED state -> ARCHIVED. */
    public Creative archive(Long id) {
        Creative creative = getCreativeById(id);
        requireTransitionFrom(creative.getStatus(), EnumSet.complementOf(EnumSet.of(CreativeStatus.ARCHIVED)));
        creative.setStatus(CreativeStatus.ARCHIVED);
        return creativeRepository.save(creative);
    }

    private void requireTransitionFrom(CreativeStatus current, Set<CreativeStatus> allowed) {
        if (!allowed.contains(current)) {
            throw new IllegalStateException("Cannot transition creative from " + current);
        }
    }

    private Campaign resolveCampaign(Long campaignId, Long tenantId) {
        return campaignRepository.findByIdAndTenantId(campaignId, tenantId)
                .orElseThrow(() -> new NotFoundException("Campaign not found: " + campaignId));
    }
}
