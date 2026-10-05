package com.adc.adcampaign.bidding;

import com.adc.adcampaign.bidding.model.AuctionRequest;
import com.adc.adcampaign.bidding.model.BidDecision;
import com.adc.adcampaign.bidding.model.ImpressionOpportunity;
import com.adc.adcampaign.bidding.repository.BidDecisionLogRepository;
import com.adc.adcampaign.campaign.entity.*;
import com.adc.adcampaign.campaign.repository.*;
import com.adc.adcampaign.campaign.service.AdImpressionService;
import com.adc.adcampaign.finance.billing.BillingService;
import com.adc.adcampaign.finance.payments.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class BiddingServiceTest {

    private static final Long TENANT_ID = 10L;

    @Mock private CampaignRepository campaignRepository;
    @Mock private CreativeRepository creativeRepository;
    @Mock private TargetingRuleRepository targetingRuleRepository;
    @Mock private BudgetRepository budgetRepository;
    @Mock private AdImpressionService adImpressionService;
    @Mock private FrequencyCapService frequencyCapService;
    @Mock private PacingService pacingService;
    @Mock private BidDecisionLogRepository bidDecisionLogRepository;
    @Mock private BillingService billingService;
    @Mock private AccountService accountService;

    private BiddingService biddingService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        biddingService = new BiddingService(campaignRepository, creativeRepository, targetingRuleRepository,
                budgetRepository, adImpressionService, frequencyCapService, pacingService, bidDecisionLogRepository,
                billingService, accountService);
    }

    private Campaign activeCampaign(long id, double bidPrice) {
        long advertiserId = id * 100;
        when(campaignRepository.findAdvertiserIdById(id)).thenReturn(Optional.of(advertiserId));
        when(accountService.hasFunds(TENANT_ID, advertiserId)).thenReturn(true);
        return Campaign.builder()
                .id(id)
                .name("Campaign " + id)
                .status(CampaignStatus.ACTIVE)
                .bidPrice(BigDecimal.valueOf(bidPrice))
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(1))
                .build();
    }

    private Budget openBudget(Campaign campaign) {
        return Budget.builder()
                .id(campaign.getId())
                .campaign(campaign)
                .totalBudget(BigDecimal.valueOf(1000.0))
                .spentAmount(BigDecimal.ZERO)
                .pacingMode("ACCELERATED")
                .build();
    }

    private Creative approvedCreative(Campaign campaign) {
        return Creative.builder()
                .id(campaign.getId() * 10)
                .campaign(campaign)
                .status(CreativeStatus.APPROVED)
                .url("https://cdn.example/creative.jpg")
                .width(300)
                .height(250)
                .build();
    }

    private AuctionRequest auctionRequest() {
        return AuctionRequest.builder()
                .requestId("req-1")
                .exchange("bidscube")
                .tenantId(TENANT_ID)
                .impressions(List.of(ImpressionOpportunity.builder().impId("imp-1").width(300).height(250).build()))
                .userId("user-1")
                .deadlineNanos(System.nanoTime() + 50_000_000L)
                .build();
    }

    @Test
    void returnsBidForEligibleCampaign() {
        Campaign campaign = activeCampaign(1L, 2.5);
        Budget budget = openBudget(campaign);
        Creative creative = approvedCreative(campaign);

        when(campaignRepository.findByTenantId(TENANT_ID)).thenReturn(List.of(campaign));
        when(targetingRuleRepository.findByCampaignId(1L)).thenReturn(List.of());
        when(budgetRepository.findByCampaignId(1L)).thenReturn(Optional.of(budget));
        when(pacingService.hasBudget(budget)).thenReturn(true);
        when(creativeRepository.findByCampaignId(1L)).thenReturn(List.of(creative));
        when(frequencyCapService.tryConsume(eq(1L), eq("user-1"), anyInt())).thenReturn(true);

        Optional<BidDecision> decision = biddingService.decide(auctionRequest());

        assertThat(decision).isPresent();
        assertThat(decision.get().getCampaignId()).isEqualTo(1L);
        assertThat(decision.get().getPrice()).isEqualByComparingTo("2.5");
        assertThat(decision.get().getSourceEventId()).isEqualTo("req-1:imp-1");
        verify(billingService).chargeForWin(eq(TENANT_ID), eq(1L), eq(budget.getId()), eq("req-1:imp-1"),
                eq(BigDecimal.valueOf(2.5)));
        verify(adImpressionService).logImpression(any());
        verify(bidDecisionLogRepository).save(argThat(log -> "BID".equals(log.getOutcome())));
    }

    @Test
    void noBidWhenNoCampaignIsEligible() {
        when(campaignRepository.findByTenantId(TENANT_ID)).thenReturn(List.of());

        Optional<BidDecision> decision = biddingService.decide(auctionRequest());

        assertThat(decision).isEmpty();
        verify(bidDecisionLogRepository).save(argThat(log ->
                "NO_BID".equals(log.getOutcome()) && "NO_ACTIVE_CAMPAIGN".equals(log.getReasonCode())));
        verifyNoInteractions(billingService);
    }

    @Test
    void noBidWhenAdvertiserHasNoFunds() {
        Campaign campaign = activeCampaign(1L, 2.5);
        Budget budget = openBudget(campaign);
        Creative creative = approvedCreative(campaign);

        when(campaignRepository.findByTenantId(TENANT_ID)).thenReturn(List.of(campaign));
        when(targetingRuleRepository.findByCampaignId(1L)).thenReturn(List.of());
        when(budgetRepository.findByCampaignId(1L)).thenReturn(Optional.of(budget));
        when(pacingService.hasBudget(budget)).thenReturn(true);
        // Overrides the default stub from activeCampaign(): this advertiser has no funds.
        when(accountService.hasFunds(TENANT_ID, 100L)).thenReturn(false);

        Optional<BidDecision> decision = biddingService.decide(auctionRequest());

        assertThat(decision).isEmpty();
        verify(bidDecisionLogRepository).save(argThat(log ->
                "NO_BID".equals(log.getOutcome()) && "NO_ACTIVE_CAMPAIGN".equals(log.getReasonCode())));
        verifyNoInteractions(billingService);
        verify(creativeRepository, never()).findByCampaignId(any());
    }

    @Test
    void noBidWhenFrequencyCapped() {
        Campaign campaign = activeCampaign(1L, 2.5);
        Budget budget = openBudget(campaign);
        Creative creative = approvedCreative(campaign);

        when(campaignRepository.findByTenantId(TENANT_ID)).thenReturn(List.of(campaign));
        when(targetingRuleRepository.findByCampaignId(1L)).thenReturn(List.of());
        when(budgetRepository.findByCampaignId(1L)).thenReturn(Optional.of(budget));
        when(pacingService.hasBudget(budget)).thenReturn(true);
        when(creativeRepository.findByCampaignId(1L)).thenReturn(List.of(creative));
        when(frequencyCapService.tryConsume(eq(1L), eq("user-1"), anyInt())).thenReturn(false);

        Optional<BidDecision> decision = biddingService.decide(auctionRequest());

        assertThat(decision).isEmpty();
        verify(bidDecisionLogRepository).save(argThat(log ->
                "NO_BID".equals(log.getOutcome()) && "FREQUENCY_CAPPED".equals(log.getReasonCode())));
        verifyNoInteractions(billingService);
    }

    @Test
    void noBidWhenDeadlineAlreadyExceeded() {
        AuctionRequest request = AuctionRequest.builder()
                .requestId("req-1")
                .exchange("bidscube")
                .tenantId(TENANT_ID)
                .impressions(List.of(ImpressionOpportunity.builder().impId("imp-1").build()))
                .userId("user-1")
                .deadlineNanos(System.nanoTime() - 1_000_000L)
                .build();

        Optional<BidDecision> decision = biddingService.decide(request);

        assertThat(decision).isEmpty();
        verifyNoInteractions(campaignRepository);
        verify(bidDecisionLogRepository).save(argThat(log ->
                "NO_BID".equals(log.getOutcome()) && "DEADLINE_EXCEEDED".equals(log.getReasonCode())));
    }

    @Test
    void neverChargesWhenDeadlineLapsesBetweenFrequencyCapAndCharging() throws InterruptedException {
        // Simulates the HTTP-level timeout race a slow eligibility/frequency-cap
        // step can create (see BidsCubeOpenRtbController's orTimeout): the
        // deadline is still valid at the start, but expires before the final,
        // money-committing check right before billingService.chargeForWin.
        Campaign campaign = activeCampaign(1L, 2.5);
        Budget budget = openBudget(campaign);
        Creative creative = approvedCreative(campaign);

        when(campaignRepository.findByTenantId(TENANT_ID)).thenReturn(List.of(campaign));
        when(targetingRuleRepository.findByCampaignId(1L)).thenReturn(List.of());
        when(budgetRepository.findByCampaignId(1L)).thenReturn(Optional.of(budget));
        when(pacingService.hasBudget(budget)).thenReturn(true);
        when(creativeRepository.findByCampaignId(1L)).thenReturn(List.of(creative));
        when(frequencyCapService.tryConsume(eq(1L), eq("user-1"), anyInt())).thenAnswer(invocation -> {
            Thread.sleep(30);
            return true;
        });

        AuctionRequest request = AuctionRequest.builder()
                .requestId("req-1")
                .exchange("bidscube")
                .tenantId(TENANT_ID)
                .impressions(List.of(ImpressionOpportunity.builder().impId("imp-1").width(300).height(250).build()))
                .userId("user-1")
                .deadlineNanos(System.nanoTime() + 15_000_000L) // 15ms, shorter than the 30ms sleep above
                .build();

        Optional<BidDecision> decision = biddingService.decide(request);

        assertThat(decision).isEmpty();
        verifyNoInteractions(billingService);
        verify(bidDecisionLogRepository).save(argThat(log ->
                "NO_BID".equals(log.getOutcome()) && "DEADLINE_EXCEEDED".equals(log.getReasonCode())));
    }
}
