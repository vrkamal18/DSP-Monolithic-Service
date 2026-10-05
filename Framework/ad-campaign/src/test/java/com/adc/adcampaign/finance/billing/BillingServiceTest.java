package com.adc.adcampaign.finance.billing;

import com.adc.adcampaign.bidding.PacingService;
import com.adc.adcampaign.campaign.entity.Advertiser;
import com.adc.adcampaign.campaign.entity.Budget;
import com.adc.adcampaign.campaign.entity.Campaign;
import com.adc.adcampaign.campaign.repository.BudgetRepository;
import com.adc.adcampaign.campaign.repository.CampaignRepository;
import com.adc.adcampaign.finance.billing.entity.LedgerEntry;
import com.adc.adcampaign.finance.billing.entity.LedgerEntryType;
import com.adc.adcampaign.finance.billing.repository.LedgerEntryRepository;
import com.adc.adcampaign.finance.payments.AccountService;
import com.adc.adcampaign.finance.pricing.FeeCalculationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit-tests the fee/idempotency branching in BillingService by stubbing
 * JdbcTemplate's insert result directly. The raw SQL itself (the
 * ON CONFLICT ... DO NOTHING RETURNING id upsert) is verified live against
 * real Postgres in the phase-3 smoke test, matching how
 * bidding/PacingService's and bidding/FrequencyCapService's equivalent raw
 * SQL is verified elsewhere in this codebase.
 */
class BillingServiceTest {

    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private FeeCalculationService feeCalculationService;
    @Mock private PacingService pacingService;
    @Mock private LedgerEntryRepository ledgerEntryRepository;
    @Mock private BudgetRepository budgetRepository;
    @Mock private CampaignRepository campaignRepository;
    @Mock private AccountService accountService;

    private BillingService billingService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        billingService = new BillingService(jdbcTemplate, feeCalculationService, pacingService,
                ledgerEntryRepository, budgetRepository, campaignRepository, accountService);
    }

    @SuppressWarnings("unchecked")
    private void stubChargeInsertResult(Long returnedId) {
        // chargeForWin's INSERT passes 8 varargs: tenantId, campaignId,
        // sourceEventId, mediaCost, platformFee, totalAmount, currency, timestamp.
        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class),
                any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(returnedId);
    }

    @SuppressWarnings("unchecked")
    private void stubAdjustmentInsertResult(Long returnedId) {
        // recordAdjustment's INSERT passes 7 varargs: tenantId, campaignId,
        // idempotencyKey, amount, currency, userId, timestamp.
        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class),
                any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(returnedId);
    }

    @Test
    void chargeForWinRecordsGrossSpendOnFirstInsert() {
        Campaign campaign = Campaign.builder().id(5L).advertiser(Advertiser.builder().id(9L).build()).build();
        when(feeCalculationService.calculatePlatformFee(BigDecimal.valueOf(2.0), 1L))
                .thenReturn(BigDecimal.valueOf(0.2));
        when(campaignRepository.findByIdAndTenantId(5L, 1L)).thenReturn(Optional.of(campaign));
        stubChargeInsertResult(99L);

        billingService.chargeForWin(1L, 5L, 7L, "req-1:imp-1", BigDecimal.valueOf(2.0));

        verify(pacingService).recordSpend(7L, 5L, BigDecimal.valueOf(2.2));
        verify(accountService).debitForSpend(1L, 9L, BigDecimal.valueOf(2.2), "req-1:imp-1");
    }

    @Test
    void chargeForWinIsANoOpOnDuplicateSourceEventId() {
        when(feeCalculationService.calculatePlatformFee(any(), any())).thenReturn(BigDecimal.ZERO);
        stubChargeInsertResult(null);

        billingService.chargeForWin(1L, 5L, 7L, "req-1:imp-1", BigDecimal.valueOf(2.0));

        verify(pacingService, never()).recordSpend(any(), any(), any());
        verifyNoInteractions(campaignRepository, accountService);
    }

    @Test
    void recordAdjustmentInsertsOnFirstCall() {
        stubAdjustmentInsertResult(1L);

        billingService.recordAdjustment(1L, 5L, BigDecimal.valueOf(-10), "adj-key-1", 42L);

        verify(jdbcTemplate).query(anyString(), any(ResultSetExtractor.class),
                any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void recordAdjustmentIsANoOpOnDuplicateIdempotencyKey() {
        stubAdjustmentInsertResult(null);

        billingService.recordAdjustment(1L, 5L, BigDecimal.valueOf(-10), "adj-key-1", 42L);

        verify(pacingService, never()).recordSpend(any(), any(), any());
    }

    private LedgerEntry originalCharge() {
        return LedgerEntry.builder()
                .id(1L)
                .tenantId(1L)
                .campaignId(5L)
                .entryType(LedgerEntryType.CHARGE)
                .sourceEventId("req-1:imp-1")
                .mediaCost(BigDecimal.valueOf(2.0))
                .platformFee(BigDecimal.valueOf(0.2))
                .totalAmount(BigDecimal.valueOf(2.2))
                .currency("USD")
                .build();
    }

    @Test
    void reconcileWinAppliesNegativeDeltaWhenActualPriceIsLower() {
        when(ledgerEntryRepository.findByTenantIdAndSourceEventId(1L, "req-1:imp-1"))
                .thenReturn(Optional.of(originalCharge()));
        when(feeCalculationService.calculatePlatformFee(BigDecimal.valueOf(1.5), 1L))
                .thenReturn(BigDecimal.valueOf(0.15));
        when(budgetRepository.findByCampaignIdAndTenantId(5L, 1L))
                .thenReturn(Optional.of(Budget.builder().id(7L).build()));
        stubAdjustmentInsertResult(2L);

        billingService.reconcileWin(1L, "req-1:imp-1", BigDecimal.valueOf(1.5));

        // new total 1.65 - original total 2.2 = -0.55
        verify(pacingService).recordSpend(7L, 5L, BigDecimal.valueOf(1.65).subtract(BigDecimal.valueOf(2.2)));
    }

    @Test
    void reconcileWinIsANoOpWhenSourceEventIsUnknown() {
        when(ledgerEntryRepository.findByTenantIdAndSourceEventId(1L, "unknown-event"))
                .thenReturn(Optional.empty());

        billingService.reconcileWin(1L, "unknown-event", BigDecimal.valueOf(1.5));

        verifyNoInteractions(budgetRepository, pacingService);
    }

    @Test
    void reconcileWinIsANoOpOnDuplicateWinNotice() {
        when(ledgerEntryRepository.findByTenantIdAndSourceEventId(1L, "req-1:imp-1"))
                .thenReturn(Optional.of(originalCharge()));
        when(feeCalculationService.calculatePlatformFee(any(), any())).thenReturn(BigDecimal.ZERO);
        stubAdjustmentInsertResult(null);

        billingService.reconcileWin(1L, "req-1:imp-1", BigDecimal.valueOf(1.5));

        verify(pacingService, never()).recordSpend(any(), any(), any());
    }
}
