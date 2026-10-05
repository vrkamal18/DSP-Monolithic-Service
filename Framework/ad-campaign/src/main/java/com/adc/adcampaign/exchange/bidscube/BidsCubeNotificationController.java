package com.adc.adcampaign.exchange.bidscube;

import com.adc.adcampaign.finance.billing.BillingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * BidsCube's win-notice callback (the {@code burl} the bid response points
 * to). Real exchanges fire this as a plain GET pixel with no custom headers,
 * so it isn't behind {@link BidsCubeAuthFilter} — the embedded {@code event}
 * id (unguessable without having seen the real bid request/response) is the
 * accepted security model for now; see the phase-4 plan.
 */
@RestController
@RequestMapping("/exchange/bidscube")
public class BidsCubeNotificationController {

    private final BillingService billingService;
    private final BidsCubeProperties properties;

    public BidsCubeNotificationController(BillingService billingService, BidsCubeProperties properties) {
        this.billingService = billingService;
        this.properties = properties;
    }

    @GetMapping("/win")
    public ResponseEntity<Void> winNotice(@RequestParam(required = false) String event,
                                           @RequestParam(required = false) String price) {
        BigDecimal actualPrice = parsePrice(price);
        if (event != null && !event.isBlank() && actualPrice != null) {
            billingService.reconcileWin(properties.getTenantId(), event, actualPrice);
        }
        // Always 200 — exchanges expect win pixels to succeed, not error,
        // even for an event we don't recognize or can't parse.
        return ResponseEntity.ok().build();
    }

    private BigDecimal parsePrice(String price) {
        if (price == null || price.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(price);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
