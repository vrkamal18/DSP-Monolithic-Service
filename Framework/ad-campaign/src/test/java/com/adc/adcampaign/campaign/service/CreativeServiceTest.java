package com.adc.adcampaign.campaign.service;

import com.adc.adcampaign.campaign.entity.Creative;
import com.adc.adcampaign.campaign.entity.CreativeStatus;
import com.adc.adcampaign.campaign.repository.CampaignRepository;
import com.adc.adcampaign.campaign.repository.CreativeRepository;
import com.adc.adcampaign.foundation.security.TenantPrincipal;
import com.adc.adcampaign.identity.repository.TenantRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class CreativeServiceTest {

    private static final Long TENANT_ID = 1L;
    private static final Long USER_ID = 42L;

    @Mock private CreativeRepository creativeRepository;
    @Mock private CampaignRepository campaignRepository;
    @Mock private TenantRepository tenantRepository;

    private CreativeService creativeService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        creativeService = new CreativeService(creativeRepository, campaignRepository, tenantRepository);

        TenantPrincipal principal = new TenantPrincipal(USER_ID, TENANT_ID, Set.of("CREATIVE_REVIEWER"));
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));

        when(creativeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private Creative creativeWithStatus(CreativeStatus status) {
        return Creative.builder().id(1L).status(status).build();
    }

    @Test
    void submitMovesDraftToPendingReview() {
        when(creativeRepository.findByIdAndTenantId(1L, TENANT_ID))
                .thenReturn(Optional.of(creativeWithStatus(CreativeStatus.DRAFT)));

        Creative result = creativeService.submit(1L);

        assertThat(result.getStatus()).isEqualTo(CreativeStatus.PENDING_REVIEW);
    }

    @Test
    void approveMovesPendingReviewToApprovedAndRecordsReviewer() {
        when(creativeRepository.findByIdAndTenantId(1L, TENANT_ID))
                .thenReturn(Optional.of(creativeWithStatus(CreativeStatus.PENDING_REVIEW)));

        Creative result = creativeService.approve(1L);

        assertThat(result.getStatus()).isEqualTo(CreativeStatus.APPROVED);
        assertThat(result.getReviewedByUserId()).isEqualTo(USER_ID);
        assertThat(result.getReviewedAt()).isNotNull();
    }

    @Test
    void approveFailsWhenNotPendingReview() {
        when(creativeRepository.findByIdAndTenantId(1L, TENANT_ID))
                .thenReturn(Optional.of(creativeWithStatus(CreativeStatus.DRAFT)));

        assertThatThrownBy(() -> creativeService.approve(1L)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectMovesPendingReviewToRejectedWithReason() {
        when(creativeRepository.findByIdAndTenantId(1L, TENANT_ID))
                .thenReturn(Optional.of(creativeWithStatus(CreativeStatus.PENDING_REVIEW)));

        Creative result = creativeService.reject(1L, "Low resolution asset");

        assertThat(result.getStatus()).isEqualTo(CreativeStatus.REJECTED);
        assertThat(result.getRejectionReason()).isEqualTo("Low resolution asset");
        assertThat(result.getReviewedByUserId()).isEqualTo(USER_ID);
    }

    @Test
    void rejectedCreativeCanBeResubmitted() {
        when(creativeRepository.findByIdAndTenantId(1L, TENANT_ID))
                .thenReturn(Optional.of(creativeWithStatus(CreativeStatus.REJECTED)));

        Creative result = creativeService.submit(1L);

        assertThat(result.getStatus()).isEqualTo(CreativeStatus.PENDING_REVIEW);
        assertThat(result.getRejectionReason()).isNull();
    }
}
