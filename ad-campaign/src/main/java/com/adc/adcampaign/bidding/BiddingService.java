package com.adc.adcampaign.bidding;

import com.adc.adcampaign.bidding.entity.BidDecisionLog;
import com.adc.adcampaign.bidding.model.AuctionRequest;
import com.adc.adcampaign.bidding.model.BidDecision;
import com.adc.adcampaign.bidding.model.ImpressionOpportunity;
import com.adc.adcampaign.bidding.model.NoBidReason;
import com.adc.adcampaign.bidding.repository.BidDecisionLogRepository;
import com.adc.adcampaign.finance.billing.BillingService;
import com.adc.adcampaign.finance.payments.AccountService;
import com.adc.adcampaign.campaign.entity.*;
import com.adc.adcampaign.campaign.repository.*;
import com.adc.adcampaign.campaign.service.AdImpressionService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Vendor-neutral bid decisioning: eligibility, targeting, creative selection,
 * frequency cap, pacing, budget authorization, and price calculation.
 *
 * Consumes only the normalized {@link AuctionRequest} — no OpenRTB or
 * exchange-specific types leak in here, so a second exchange adapter can
 * reuse this unchanged.
 */
@Service
public class BiddingService {

    private static final int MAX_IMPRESSIONS_PER_DAY = 5;

    private final CampaignRepository campaignRepository;
    private final CreativeRepository creativeRepository;
    private final TargetingRuleRepository targetingRuleRepository;
    private final BudgetRepository budgetRepository;
    private final AdImpressionService adImpressionService;
    private final FrequencyCapService frequencyCapService;
    private final PacingService pacingService;
    private final BidDecisionLogRepository bidDecisionLogRepository;
    private final BillingService billingService;
    private final AccountService accountService;

    public BiddingService(CampaignRepository campaignRepository,
                           CreativeRepository creativeRepository,
                           TargetingRuleRepository targetingRuleRepository,
                           BudgetRepository budgetRepository,
                           AdImpressionService adImpressionService,
                           FrequencyCapService frequencyCapService,
                           PacingService pacingService,
                           BidDecisionLogRepository bidDecisionLogRepository,
                           BillingService billingService,
                           AccountService accountService) {
        this.campaignRepository = campaignRepository;
        this.creativeRepository = creativeRepository;
        this.targetingRuleRepository = targetingRuleRepository;
        this.budgetRepository = budgetRepository;
        this.adImpressionService = adImpressionService;
        this.frequencyCapService = frequencyCapService;
        this.pacingService = pacingService;
        this.bidDecisionLogRepository = bidDecisionLogRepository;
        this.billingService = billingService;
        this.accountService = accountService;
    }

    public Optional<BidDecision> decide(AuctionRequest auctionRequest) {
        if (auctionRequest.getImpressions() == null || auctionRequest.getImpressions().isEmpty()) {
            return noBid(auctionRequest, null, NoBidReason.INVALID_REQUEST);
        }
        // Single-impression decisioning for the first pass; multi-impression
        // per request is a future enhancement.
        ImpressionOpportunity impression = auctionRequest.getImpressions().get(0);

        if (deadlineExceeded(auctionRequest)) {
            return noBid(auctionRequest, impression, NoBidReason.DEADLINE_EXCEEDED);
        }

        List<Campaign> eligible = new ArrayList<>();
        for (Campaign campaign : campaignRepository.findByTenantId(auctionRequest.getTenantId())) {
            if (!isActiveAndScheduled(campaign)) continue;
            if (!matchesTargeting(auctionRequest, targetingRuleRepository.findByCampaignId(campaign.getId()))) continue;

            Optional<Budget> budgetOpt = budgetRepository.findByCampaignId(campaign.getId());
            if (budgetOpt.isEmpty() || !pacingService.hasBudget(budgetOpt.get())) continue;

            Optional<Long> advertiserId = campaignRepository.findAdvertiserIdById(campaign.getId());
            if (advertiserId.isEmpty() || !accountService.hasFunds(auctionRequest.getTenantId(), advertiserId.get())) {
                continue;
            }

            boolean hasApprovedCreative = creativeRepository.findByCampaignId(campaign.getId()).stream()
                    .anyMatch(c -> c.getStatus() == CreativeStatus.APPROVED);
            if (!hasApprovedCreative) continue;

            eligible.add(campaign);
        }

        if (eligible.isEmpty()) {
            return noBid(auctionRequest, impression, NoBidReason.NO_ACTIVE_CAMPAIGN);
        }

        eligible.sort((c1, c2) -> Optional.ofNullable(c2.getBidPrice()).orElse(BigDecimal.ZERO)
                .compareTo(Optional.ofNullable(c1.getBidPrice()).orElse(BigDecimal.ZERO)));
        Campaign winner = eligible.get(0);

        if (!frequencyCapService.tryConsume(winner.getId(), auctionRequest.getUserId(), MAX_IMPRESSIONS_PER_DAY)) {
            return noBid(auctionRequest, impression, NoBidReason.FREQUENCY_CAPPED, winner.getId());
        }

        if (deadlineExceeded(auctionRequest)) {
            return noBid(auctionRequest, impression, NoBidReason.DEADLINE_EXCEEDED, winner.getId());
        }

        Creative creative = creativeRepository.findByCampaignId(winner.getId()).stream()
                .filter(c -> c.getStatus() == CreativeStatus.APPROVED)
                .findFirst()
                .orElseThrow(); // presence already checked above

        BigDecimal price = Optional.ofNullable(winner.getBidPrice()).orElse(BigDecimal.ZERO);

        // Guards the actual financial commitment: if we're already past deadline
        // by the time we get here, the HTTP response has likely already timed
        // out (see BidsCubeOpenRtbController) — don't charge for a bid we've
        // already told the exchange no-bid on.
        if (deadlineExceeded(auctionRequest)) {
            return noBid(auctionRequest, impression, NoBidReason.DEADLINE_EXCEEDED, winner.getId());
        }

        Budget budget = budgetRepository.findByCampaignId(winner.getId()).orElseThrow();
        String sourceEventId = auctionRequest.getRequestId() + ":" + impression.getImpId();
        billingService.chargeForWin(auctionRequest.getTenantId(), winner.getId(), budget.getId(),
                sourceEventId, price);

        AdImpression impressionRecord = AdImpression.builder()
                .campaign(winner)
                .creative(creative)
                .deviceType(auctionRequest.getDeviceType())
                .location(auctionRequest.getGeoCountry())
                .userId(auctionRequest.getUserId())
                .build();
        adImpressionService.logImpression(impressionRecord);

        logDecision(auctionRequest, winner.getId(), "BID", null, price);

        return Optional.of(BidDecision.builder()
                .impId(impression.getImpId())
                .campaignId(winner.getId())
                .creativeId(creative.getId())
                .price(price)
                .sourceEventId(sourceEventId)
                .adMarkup(creative.getUrl())
                .width(creative.getWidth())
                .height(creative.getHeight())
                .build());
    }

    private boolean isActiveAndScheduled(Campaign campaign) {
        if (campaign.getStatus() != CampaignStatus.ACTIVE) return false;
        LocalDateTime now = LocalDateTime.now();
        if (campaign.getStartDate() != null && now.isBefore(campaign.getStartDate())) return false;
        if (campaign.getEndDate() != null && now.isAfter(campaign.getEndDate())) return false;
        return true;
    }

    private boolean matchesTargeting(AuctionRequest request, List<TargetingRule> rules) {
        for (TargetingRule rule : rules) {
            if (!"include".equalsIgnoreCase(rule.getConditionType())) continue;

            if ("location".equalsIgnoreCase(rule.getRuleType())
                    && !Objects.equals(rule.getRuleValue(), request.getGeoCountry())) {
                return false;
            }
            if ("device".equalsIgnoreCase(rule.getRuleType())
                    && !Objects.equals(rule.getRuleValue(), request.getDeviceType())) {
                return false;
            }
        }
        return true;
    }

    private boolean deadlineExceeded(AuctionRequest request) {
        return request.getDeadlineNanos() > 0 && System.nanoTime() > request.getDeadlineNanos();
    }

    private Optional<BidDecision> noBid(AuctionRequest request, ImpressionOpportunity impression, NoBidReason reason) {
        return noBid(request, impression, reason, null);
    }

    private Optional<BidDecision> noBid(AuctionRequest request, ImpressionOpportunity impression,
                                         NoBidReason reason, Long campaignId) {
        logDecision(request, campaignId, "NO_BID", reason.name(), null);
        return Optional.empty();
    }

    private void logDecision(AuctionRequest request, Long campaignId, String outcome, String reasonCode, BigDecimal price) {
        bidDecisionLogRepository.save(BidDecisionLog.builder()
                .bidRequestId(request.getRequestId())
                .campaignId(campaignId)
                .exchange(request.getExchange())
                .outcome(outcome)
                .reasonCode(reasonCode)
                .price(price)
                .build());
    }
}
