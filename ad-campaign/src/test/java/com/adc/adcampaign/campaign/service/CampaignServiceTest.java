package com.adc.adcampaign.campaign.service;

import com.adc.adcampaign.campaign.dto.CampaignRequest;
import com.adc.adcampaign.campaign.entity.*;
import com.adc.adcampaign.campaign.repository.AdvertiserRepository;
import com.adc.adcampaign.campaign.repository.BudgetRepository;
import com.adc.adcampaign.campaign.repository.CampaignRepository;
import com.adc.adcampaign.campaign.repository.CampaignStatusChangeRepository;
import com.adc.adcampaign.campaign.repository.CreativeRepository;
import com.adc.adcampaign.foundation.security.TenantPrincipal;
import com.adc.adcampaign.identity.entity.Tenant;
import com.adc.adcampaign.identity.repository.TenantRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CampaignServiceTest {

    private static final Long TENANT_ID = 1L;
    private static final Long USER_ID = 42L;

    @Mock private CampaignRepository campaignRepository;
    @Mock private AdvertiserRepository advertiserRepository;
    @Mock private BudgetRepository budgetRepository;
    @Mock private CreativeRepository creativeRepository;
    @Mock private CampaignStatusChangeRepository campaignStatusChangeRepository;
    @Mock private TenantRepository tenantRepository;

    private CampaignService campaignService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        campaignService = new CampaignService(campaignRepository, advertiserRepository, budgetRepository,
                creativeRepository, campaignStatusChangeRepository, tenantRepository);

        TenantPrincipal principal = new TenantPrincipal(USER_ID, TENANT_ID, Set.of("TENANT_ADMIN"));
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));

        when(campaignRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private Campaign draftCampaign(boolean withAdvertiser, boolean validSchedule) {
        return Campaign.builder()
                .id(1L)
                .tenant(Tenant.builder().id(TENANT_ID).build())
                .name("Test Campaign")
                .status(CampaignStatus.DRAFT)
                .advertiser(withAdvertiser ? Advertiser.builder().id(9L).build() : null)
                .startDate(validSchedule ? LocalDateTime.now().minusDays(1) : null)
                .endDate(validSchedule ? LocalDateTime.now().plusDays(1) : null)
                .build();
    }

    private Budget fundedBudget(Campaign campaign) {
        return Budget.builder().id(5L).campaign(campaign).totalBudget(BigDecimal.valueOf(100)).build();
    }

    private Creative approvedCreative(Campaign campaign) {
        return Creative.builder().id(7L).campaign(campaign).status(CreativeStatus.APPROVED).build();
    }

    @Test
    void activateSucceedsWhenAllEligibilityChecksPass() {
        Campaign campaign = draftCampaign(true, true);
        when(campaignRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(campaign));
        when(budgetRepository.findByCampaignIdAndTenantId(1L, TENANT_ID))
                .thenReturn(Optional.of(fundedBudget(campaign)));
        when(creativeRepository.findByCampaignIdAndTenantId(1L, TENANT_ID))
                .thenReturn(List.of(approvedCreative(campaign)));

        Campaign result = campaignService.activate(1L);

        assertThat(result.getStatus()).isEqualTo(CampaignStatus.ACTIVE);
        verify(campaignStatusChangeRepository).save(argThatToStatusIs(CampaignStatus.ACTIVE));
    }

    @Test
    void activateFailsWithoutAnAdvertiser() {
        Campaign campaign = draftCampaign(false, true);
        when(campaignRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(campaign));

        assertThatThrownBy(() -> campaignService.activate(1L)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void activateFailsWithoutABudget() {
        Campaign campaign = draftCampaign(true, true);
        when(campaignRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(campaign));
        when(budgetRepository.findByCampaignIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> campaignService.activate(1L)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void activateFailsWithoutAnApprovedCreative() {
        Campaign campaign = draftCampaign(true, true);
        when(campaignRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(campaign));
        when(budgetRepository.findByCampaignIdAndTenantId(1L, TENANT_ID))
                .thenReturn(Optional.of(fundedBudget(campaign)));
        when(creativeRepository.findByCampaignIdAndTenantId(1L, TENANT_ID)).thenReturn(List.of());

        assertThatThrownBy(() -> campaignService.activate(1L)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void activateFailsWithoutAValidSchedule() {
        Campaign campaign = draftCampaign(true, false);
        when(campaignRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(campaign));
        when(budgetRepository.findByCampaignIdAndTenantId(1L, TENANT_ID))
                .thenReturn(Optional.of(fundedBudget(campaign)));
        when(creativeRepository.findByCampaignIdAndTenantId(1L, TENANT_ID))
                .thenReturn(List.of(approvedCreative(campaign)));

        assertThatThrownBy(() -> campaignService.activate(1L)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void invalidTransitionIsRejected() {
        Campaign campaign = draftCampaign(true, true);
        campaign.setStatus(CampaignStatus.DRAFT);
        when(campaignRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(campaign));

        assertThatThrownBy(() -> campaignService.complete(1L)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void createIgnoresAnyClientSuppliedStatusAndStartsDraft() {
        when(tenantRepository.getReferenceById(TENANT_ID)).thenReturn(Tenant.builder().id(TENANT_ID).build());
        CampaignRequest request = new CampaignRequest();
        request.setName("New Campaign");

        Campaign created = campaignService.createCampaign(request);

        assertThat(created.getStatus()).isEqualTo(CampaignStatus.DRAFT);
    }

    private static CampaignStatusChange argThatToStatusIs(CampaignStatus status) {
        return org.mockito.ArgumentMatchers.argThat(change -> change.getToStatus() == status);
    }
}
