package com.adc.adcampaign.campaign.service;

import com.adc.adcampaign.campaign.dto.CampaignRequest;
import com.adc.adcampaign.campaign.entity.*;
import com.adc.adcampaign.campaign.repository.AdvertiserRepository;
import com.adc.adcampaign.campaign.repository.BudgetRepository;
import com.adc.adcampaign.campaign.repository.CampaignRepository;
import com.adc.adcampaign.campaign.repository.CampaignStatusChangeRepository;
import com.adc.adcampaign.campaign.repository.CreativeRepository;
import com.adc.adcampaign.foundation.error.NotFoundException;
import com.adc.adcampaign.foundation.security.TenantContext;
import com.adc.adcampaign.identity.entity.Tenant;
import com.adc.adcampaign.identity.repository.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final AdvertiserRepository advertiserRepository;
    private final BudgetRepository budgetRepository;
    private final CreativeRepository creativeRepository;
    private final CampaignStatusChangeRepository campaignStatusChangeRepository;
    private final TenantRepository tenantRepository;

    public CampaignService(CampaignRepository campaignRepository,
                            AdvertiserRepository advertiserRepository,
                            BudgetRepository budgetRepository,
                            CreativeRepository creativeRepository,
                            CampaignStatusChangeRepository campaignStatusChangeRepository,
                            TenantRepository tenantRepository) {
        this.campaignRepository = campaignRepository;
        this.advertiserRepository = advertiserRepository;
        this.budgetRepository = budgetRepository;
        this.creativeRepository = creativeRepository;
        this.campaignStatusChangeRepository = campaignStatusChangeRepository;
        this.tenantRepository = tenantRepository;
    }

    public Campaign createCampaign(CampaignRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        Tenant tenant = tenantRepository.getReferenceById(tenantId);
        Campaign campaign = Campaign.builder()
                .tenant(tenant)
                .name(request.getName())
                .bidPrice(request.getBidPrice())
                .objective(request.getObjective())
                .advertiser(resolveAdvertiser(request.getAdvertiserId(), tenantId))
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(CampaignStatus.DRAFT)
                .build();
        return campaignRepository.save(campaign);
    }

    public List<Campaign> getAllCampaigns() {
        return campaignRepository.findByTenantId(TenantContext.requireTenantId());
    }

    public Campaign getCampaignById(Long id) {
        return campaignRepository.findByIdAndTenantId(id, TenantContext.requireTenantId())
                .orElseThrow(() -> new NotFoundException("Campaign not found: " + id));
    }

    public List<Campaign> getCampaignsByAdvertiser(Long advertiserId) {
        Long tenantId = TenantContext.requireTenantId();
        return campaignRepository.findByAdvertiserId(advertiserId).stream()
                .filter(c -> c.getTenant().getId().equals(tenantId))
                .toList();
    }

    public Campaign updateCampaign(Long id, CampaignRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        Campaign existing = getCampaignById(id);
        existing.setName(request.getName());
        existing.setBidPrice(request.getBidPrice());
        existing.setObjective(request.getObjective());
        existing.setAdvertiser(resolveAdvertiser(request.getAdvertiserId(), tenantId));
        existing.setStartDate(request.getStartDate());
        existing.setEndDate(request.getEndDate());
        return campaignRepository.save(existing);
    }

    public void deleteCampaign(Long id) {
        campaignRepository.delete(getCampaignById(id));
    }

    /** DRAFT or PAUSED -> ACTIVE, after eligibility validation. */
    @Transactional
    public Campaign activate(Long id) {
        return moveToActive(id, EnumSet.of(CampaignStatus.DRAFT, CampaignStatus.PAUSED));
    }

    /** PAUSED -> ACTIVE, after the same eligibility validation as activate(). */
    @Transactional
    public Campaign resume(Long id) {
        return moveToActive(id, EnumSet.of(CampaignStatus.PAUSED));
    }

    @Transactional
    public Campaign pause(Long id) {
        Campaign campaign = getCampaignById(id);
        requireTransitionFrom(campaign.getStatus(), EnumSet.of(CampaignStatus.ACTIVE));
        return transition(campaign, CampaignStatus.PAUSED);
    }

    @Transactional
    public Campaign complete(Long id) {
        Campaign campaign = getCampaignById(id);
        requireTransitionFrom(campaign.getStatus(), EnumSet.of(CampaignStatus.ACTIVE, CampaignStatus.PAUSED));
        return transition(campaign, CampaignStatus.COMPLETED);
    }

    @Transactional
    public Campaign archive(Long id) {
        Campaign campaign = getCampaignById(id);
        requireTransitionFrom(campaign.getStatus(), EnumSet.complementOf(EnumSet.of(CampaignStatus.ARCHIVED)));
        return transition(campaign, CampaignStatus.ARCHIVED);
    }

    private Campaign moveToActive(Long id, Set<CampaignStatus> allowedFrom) {
        Long tenantId = TenantContext.requireTenantId();
        Campaign campaign = getCampaignById(id);
        requireTransitionFrom(campaign.getStatus(), allowedFrom);
        validateActivationEligibility(campaign, tenantId);
        return transition(campaign, CampaignStatus.ACTIVE);
    }

    private void validateActivationEligibility(Campaign campaign, Long tenantId) {
        if (campaign.getAdvertiser() == null) {
            throw new IllegalStateException("Campaign requires an advertiser before activation");
        }
        BigDecimal totalBudget = budgetRepository.findByCampaignIdAndTenantId(campaign.getId(), tenantId)
                .map(Budget::getTotalBudget)
                .orElse(null);
        if (totalBudget == null || totalBudget.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Campaign requires a budget with a positive total before activation");
        }
        boolean hasApprovedCreative = creativeRepository.findByCampaignIdAndTenantId(campaign.getId(), tenantId).stream()
                .anyMatch(c -> c.getStatus() == CreativeStatus.APPROVED);
        if (!hasApprovedCreative) {
            throw new IllegalStateException("Campaign requires at least one approved creative before activation");
        }
        if (campaign.getStartDate() == null || campaign.getEndDate() == null
                || campaign.getStartDate().isAfter(campaign.getEndDate())) {
            throw new IllegalStateException("Campaign requires a valid start/end schedule before activation");
        }
    }

    private void requireTransitionFrom(CampaignStatus current, Set<CampaignStatus> allowed) {
        if (!allowed.contains(current)) {
            throw new IllegalStateException("Cannot transition campaign from " + current);
        }
    }

    private Campaign transition(Campaign campaign, CampaignStatus newStatus) {
        CampaignStatus previous = campaign.getStatus();
        campaign.setStatus(newStatus);
        Campaign saved = campaignRepository.save(campaign);
        campaignStatusChangeRepository.save(CampaignStatusChange.builder()
                .campaignId(campaign.getId())
                .tenantId(TenantContext.requireTenantId())
                .fromStatus(previous)
                .toStatus(newStatus)
                .changedBy(TenantContext.requireUserId())
                .build());
        return saved;
    }

    private Advertiser resolveAdvertiser(Long advertiserId, Long tenantId) {
        if (advertiserId == null) {
            return null;
        }
        return advertiserRepository.findByIdAndTenantId(advertiserId, tenantId)
                .orElseThrow(() -> new NotFoundException("Advertiser not found: " + advertiserId));
    }
}
