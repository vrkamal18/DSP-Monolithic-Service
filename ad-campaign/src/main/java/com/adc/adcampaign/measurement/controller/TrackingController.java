package com.adc.adcampaign.measurement.controller;

import com.adc.adcampaign.measurement.ClickRedirectValidator;
import com.adc.adcampaign.measurement.MeasurementService;
import com.adc.adcampaign.measurement.dto.ViewabilityRequest;
import com.adc.adcampaign.measurement.dto.VideoQuartileRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Base64;

/**
 * Public, unauthenticated tracking pixels/beacons — see
 * service/documents/02-module-specifications.md §12. Excluded from JWT auth
 * in {@code identity.SecurityConfig} and rate-limited by
 * {@code TrackingRateLimitFilter}, not Spring Security.
 */
@RestController
@RequestMapping("/t")
public class TrackingController {

    private static final byte[] TRANSPARENT_GIF = Base64.getDecoder()
            .decode("R0lGODlhAQABAIAAAAAAAP///ywAAAAAAQABAAACAUwAOw==");

    private final MeasurementService measurementService;
    private final ClickRedirectValidator clickRedirectValidator;

    public TrackingController(MeasurementService measurementService, ClickRedirectValidator clickRedirectValidator) {
        this.measurementService = measurementService;
        this.clickRedirectValidator = clickRedirectValidator;
    }

    @GetMapping(value = "/impression", produces = MediaType.IMAGE_GIF_VALUE)
    public ResponseEntity<byte[]> impression(@RequestParam Long tenantId, @RequestParam Long campaignId,
                                              @RequestParam(required = false) Long creativeId,
                                              @RequestParam String eventId) {
        measurementService.recordImpression(tenantId, campaignId, creativeId, eventId);
        return pixel();
    }

    @GetMapping("/click")
    public ResponseEntity<?> click(@RequestParam Long tenantId, @RequestParam Long campaignId,
                                    @RequestParam(required = false) Long creativeId,
                                    @RequestParam String eventId, @RequestParam String redirectUrl) {
        if (!clickRedirectValidator.isSafe(redirectUrl)) {
            return ResponseEntity.badRequest()
                    .body(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid redirect destination"));
        }
        measurementService.recordClick(tenantId, campaignId, creativeId, eventId, redirectUrl);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, URI.create(redirectUrl).toString())
                .build();
    }

    @GetMapping(value = "/conversion", produces = MediaType.IMAGE_GIF_VALUE)
    public ResponseEntity<byte[]> conversion(@RequestParam Long tenantId, @RequestParam Long campaignId,
                                              @RequestParam(required = false) Long creativeId,
                                              @RequestParam String eventId,
                                              @RequestParam(required = false) String conversionType,
                                              @RequestParam(required = false) BigDecimal value) {
        measurementService.recordConversion(tenantId, campaignId, creativeId, eventId, conversionType, value);
        return pixel();
    }

    @PostMapping("/viewability")
    public ResponseEntity<Void> viewability(@Valid @RequestBody ViewabilityRequest request) {
        measurementService.recordViewability(request.getTenantId(), request.getCampaignId(),
                request.getCreativeId(), request.getEventId(), request.getViewableDurationMs(),
                request.getViewablePercentage());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/video-quartile")
    public ResponseEntity<Void> videoQuartile(@Valid @RequestBody VideoQuartileRequest request) {
        measurementService.recordVideoQuartile(request.getTenantId(), request.getCampaignId(),
                request.getCreativeId(), request.getEventId(), request.getQuartile());
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<byte[]> pixel() {
        return ResponseEntity.ok().contentType(MediaType.IMAGE_GIF).body(TRANSPARENT_GIF);
    }
}
