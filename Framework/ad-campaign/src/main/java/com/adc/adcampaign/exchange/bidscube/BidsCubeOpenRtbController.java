package com.adc.adcampaign.exchange.bidscube;

import com.adc.adcampaign.bidding.BiddingService;
import com.adc.adcampaign.bidding.model.AuctionRequest;
import com.adc.adcampaign.bidding.model.BidDecision;
import com.adc.adcampaign.bidding.model.NoBidReason;
import com.adc.adcampaign.foundation.web.CorrelationIdFilter;
import com.adc.adcampaign.openrtb.OpenRtbMapper;
import com.adc.adcampaign.openrtb.OpenRtbRequestValidator;
import com.adc.adcampaign.openrtb.dto.BidRequest;
import com.adc.adcampaign.openrtb.dto.BidResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * BidsCube's entry point into the vendor-neutral OpenRTB gateway. Everything
 * BidsCube-specific (auth, path, response seat name) lives here; parsing,
 * validation, and decisioning are shared with any future exchange adapter.
 *
 * Decisioning runs on a dedicated, bounded {@code openRtbExecutor} instead of
 * the Tomcat request thread, with a hard {@code tmax} deadline enforced via
 * {@link CompletableFuture#orTimeout}. Both a timeout and a full queue
 * resolve to a plain no-bid (204) — never an error — per
 * service/documents/05-openrtb-and-bidding-specification.md §8.
 */
@RestController
@RequestMapping("/exchange/bidscube")
public class BidsCubeOpenRtbController {

    private static final Logger log = LoggerFactory.getLogger(BidsCubeOpenRtbController.class);

    private static final String EXCHANGE_NAME = "bidscube";
    private static final String SEAT_NAME = "bidscube";

    private final OpenRtbRequestValidator validator;
    private final OpenRtbMapper mapper;
    private final BiddingService biddingService;
    private final BidsCubeProperties properties;
    private final ExecutorService openRtbExecutor;

    public BidsCubeOpenRtbController(OpenRtbRequestValidator validator,
                                      OpenRtbMapper mapper,
                                      BiddingService biddingService,
                                      BidsCubeProperties properties,
                                      ExecutorService openRtbExecutor) {
        this.validator = validator;
        this.mapper = mapper;
        this.biddingService = biddingService;
        this.properties = properties;
        this.openRtbExecutor = openRtbExecutor;
    }

    @PostMapping("/openrtb2/bidrequest")
    public CompletableFuture<ResponseEntity<BidResponse>> handleBidRequest(@Valid @RequestBody BidRequest request) {
        long deadlineNanos = System.nanoTime()
                + (request.getTmax() != null ? request.getTmax() : properties.getDefaultTmaxMillis()) * 1_000_000L
                - properties.getTmaxSafetyMarginMillis() * 1_000_000L;

        Optional<NoBidReason> validationFailure = validator.validate(request);
        if (validationFailure.isPresent()) {
            return CompletableFuture.completedFuture(ResponseEntity.noContent().build());
        }

        AuctionRequest auctionRequest = mapper.toAuctionRequest(
                request, EXCHANGE_NAME, properties.getTenantId(), properties.getDefaultTmaxMillis(), deadlineNanos);

        String correlationId = CorrelationIdFilter.current();
        long remainingMillis = Math.max(0, (deadlineNanos - System.nanoTime()) / 1_000_000L);

        try {
            return CompletableFuture
                    .supplyAsync(() -> biddingService.decide(auctionRequest), openRtbExecutor)
                    .orTimeout(remainingMillis, TimeUnit.MILLISECONDS)
                    .thenApply(decision -> decision
                            .map(d -> ResponseEntity.ok(mapper.toBidResponse(request.getId(), SEAT_NAME, d, burl(d))))
                            .orElseGet(() -> ResponseEntity.noContent().build()))
                    .exceptionally(ex -> {
                        log.warn("OpenRTB request {} (correlationId={}) missed its {}ms deadline",
                                request.getId(), correlationId, remainingMillis);
                        return ResponseEntity.noContent().build();
                    });
        } catch (RejectedExecutionException e) {
            log.warn("OpenRTB executor overloaded, rejecting request {} (correlationId={})",
                    request.getId(), correlationId);
            return CompletableFuture.completedFuture(ResponseEntity.noContent().build());
        }
    }

    private String burl(BidDecision decision) {
        String base = properties.getWinNoticeBaseUrl();
        if (base == null || base.isBlank()) {
            return null;
        }
        String event = URLEncoder.encode(decision.getSourceEventId(), StandardCharsets.UTF_8);
        return base + "/exchange/bidscube/win?event=" + event + "&price=${AUCTION_PRICE}";
    }
}
