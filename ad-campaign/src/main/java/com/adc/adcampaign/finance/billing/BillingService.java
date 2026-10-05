package com.adc.adcampaign.finance.billing;

import com.adc.adcampaign.bidding.PacingService;
import com.adc.adcampaign.campaign.entity.Budget;
import com.adc.adcampaign.campaign.entity.Campaign;
import com.adc.adcampaign.campaign.repository.BudgetRepository;
import com.adc.adcampaign.campaign.repository.CampaignRepository;
import com.adc.adcampaign.finance.billing.entity.LedgerEntry;
import com.adc.adcampaign.finance.billing.repository.LedgerEntryRepository;
import com.adc.adcampaign.finance.payments.AccountService;
import com.adc.adcampaign.finance.pricing.FeeCalculationService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * The billing ledger: idempotent, append-only financial records for wins and
 * manual corrections. See service/documents/02-module-specifications.md §13.3
 * and the phase-3 plan for why this exists (BiddingService previously spent
 * against Budget directly with no fee and no duplicate-processing guard).
 */
@Service
public class BillingService {

    private static final String INSERT_CHARGE_SQL = """
            INSERT INTO ledger_entries
                (tenant_id, campaign_id, entry_type, source_event_id, media_cost, platform_fee,
                 total_amount, currency, created_at)
            VALUES (?, ?, 'CHARGE', ?, ?, ?, ?, ?, ?)
            ON CONFLICT (tenant_id, source_event_id) DO NOTHING
            RETURNING id
            """;

    private static final String INSERT_ADJUSTMENT_SQL = """
            INSERT INTO ledger_entries
                (tenant_id, campaign_id, entry_type, source_event_id, total_amount, currency,
                 created_by, created_at)
            VALUES (?, ?, 'ADJUSTMENT', ?, ?, ?, ?, ?)
            ON CONFLICT (tenant_id, source_event_id) DO NOTHING
            RETURNING id
            """;

    private static final String DEFAULT_CURRENCY = "USD";

    private final JdbcTemplate jdbcTemplate;
    private final FeeCalculationService feeCalculationService;
    private final PacingService pacingService;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final BudgetRepository budgetRepository;
    private final CampaignRepository campaignRepository;
    private final AccountService accountService;

    public BillingService(JdbcTemplate jdbcTemplate,
                           FeeCalculationService feeCalculationService,
                           PacingService pacingService,
                           LedgerEntryRepository ledgerEntryRepository,
                           BudgetRepository budgetRepository,
                           CampaignRepository campaignRepository,
                           AccountService accountService) {
        this.jdbcTemplate = jdbcTemplate;
        this.feeCalculationService = feeCalculationService;
        this.pacingService = pacingService;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.budgetRepository = budgetRepository;
        this.campaignRepository = campaignRepository;
        this.accountService = accountService;
    }

    /**
     * Records a charge for a won impression and — only if this is the first
     * time {@code sourceEventId} has been seen for this tenant — deducts the
     * gross total (media cost + platform fee) from the campaign's budget and
     * debits the advertiser's Payments account by the same amount (the real
     * counterpart to bidding.BiddingService's pre-bid
     * AccountService.hasFunds check). A duplicate {@code sourceEventId}
     * (e.g. an exchange retrying the same bid request) is a safe no-op: no
     * second ledger row, no double spend, no double debit.
     */
    @Transactional
    public void chargeForWin(Long tenantId, Long campaignId, Long budgetId,
                              String sourceEventId, BigDecimal mediaCost) {
        BigDecimal platformFee = feeCalculationService.calculatePlatformFee(mediaCost, tenantId);
        BigDecimal totalAmount = mediaCost.add(platformFee);

        Long insertedId = jdbcTemplate.query(INSERT_CHARGE_SQL,
                rs -> rs.next() ? rs.getLong(1) : null,
                tenantId, campaignId, sourceEventId, mediaCost, platformFee, totalAmount,
                DEFAULT_CURRENCY, Timestamp.valueOf(LocalDateTime.now()));

        if (insertedId != null) {
            pacingService.recordSpend(budgetId, campaignId, totalAmount);
            campaignRepository.findByIdAndTenantId(campaignId, tenantId)
                    .map(Campaign::getAdvertiser)
                    .ifPresent(advertiser -> accountService.debitForSpend(
                            tenantId, advertiser.getId(), totalAmount, sourceEventId));
        }
    }

    /**
     * Records a manual correction. Idempotent on {@code idempotencyKey} —
     * retrying the same request (same key) does not create a second entry.
     * Does not touch campaign budget/pacing; reconciling an adjustment
     * against a campaign's live budget is a separate, later concern.
     */
    @Transactional
    public void recordAdjustment(Long tenantId, Long campaignId, BigDecimal amount,
                                  String idempotencyKey, Long userId) {
        jdbcTemplate.query(INSERT_ADJUSTMENT_SQL,
                rs -> rs.next() ? rs.getLong(1) : null,
                tenantId, campaignId, idempotencyKey, amount, DEFAULT_CURRENCY, userId,
                Timestamp.valueOf(LocalDateTime.now()));
    }

    /**
     * Reconciles a win notice against the CHARGE already recorded optimistically
     * at bid-response time (see {@link #chargeForWin}). The actual clearing
     * price (e.g. from a second-price auction) may differ from what was
     * submitted, so this posts an ADJUSTMENT for the delta rather than
     * creating a new charge — we don't have campaign/creative/budget context
     * from a bare win-notice callback, only from the original decision.
     * An unrecognized {@code sourceEventId} (no matching CHARGE) is silently
     * ignored. Idempotent on {@code sourceEventId + ":win"} — a duplicate
     * delivery of the same win notice does not adjust twice.
     */
    @Transactional
    public void reconcileWin(Long tenantId, String sourceEventId, BigDecimal actualMediaPrice) {
        if (sourceEventId == null || actualMediaPrice == null) {
            return;
        }
        ledgerEntryRepository.findByTenantIdAndSourceEventId(tenantId, sourceEventId)
                .ifPresent(original -> applyWinReconciliation(tenantId, original, actualMediaPrice));
    }

    private void applyWinReconciliation(Long tenantId, LedgerEntry original, BigDecimal actualMediaPrice) {
        BigDecimal newFee = feeCalculationService.calculatePlatformFee(actualMediaPrice, tenantId);
        BigDecimal newTotal = actualMediaPrice.add(newFee);
        BigDecimal delta = newTotal.subtract(original.getTotalAmount());
        String adjustmentEventId = original.getSourceEventId() + ":win";

        Long insertedId = jdbcTemplate.query(INSERT_ADJUSTMENT_SQL,
                rs -> rs.next() ? rs.getLong(1) : null,
                tenantId, original.getCampaignId(), adjustmentEventId, delta, DEFAULT_CURRENCY, null,
                Timestamp.valueOf(LocalDateTime.now()));

        if (insertedId != null && original.getCampaignId() != null) {
            budgetRepository.findByCampaignIdAndTenantId(original.getCampaignId(), tenantId)
                    .map(Budget::getId)
                    .ifPresent(budgetId -> pacingService.recordSpend(budgetId, original.getCampaignId(), delta));
        }
    }

    public List<LedgerEntry> getEntriesForTenant(Long tenantId) {
        return ledgerEntryRepository.findByTenantId(tenantId);
    }

    public List<LedgerEntry> getEntriesForCampaign(Long tenantId, Long campaignId) {
        return ledgerEntryRepository.findByTenantIdAndCampaignId(tenantId, campaignId);
    }
}
