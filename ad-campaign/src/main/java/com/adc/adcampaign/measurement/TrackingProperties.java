package com.adc.adcampaign.measurement;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for the public {@code /t/**} tracking endpoints. Mirrors
 * {@code exchange.bidscube.BidsCubeProperties}'s configuration style.
 */
@Component
@ConfigurationProperties(prefix = "measurement.tracking")
public class TrackingProperties {

    /** Max requests allowed per {@link #windowSeconds} per client (keyed by remote address). */
    private int maxRequestsPerWindow = 120;

    /** Width of the rate-limit window. */
    private int windowSeconds = 60;

    /** Requests with a query string longer than this are rejected before processing. */
    private int maxQueryLength = 2048;

    public int getMaxRequestsPerWindow() {
        return maxRequestsPerWindow;
    }

    public void setMaxRequestsPerWindow(int maxRequestsPerWindow) {
        this.maxRequestsPerWindow = maxRequestsPerWindow;
    }

    public int getWindowSeconds() {
        return windowSeconds;
    }

    public void setWindowSeconds(int windowSeconds) {
        this.windowSeconds = windowSeconds;
    }

    public int getMaxQueryLength() {
        return maxQueryLength;
    }

    public void setMaxQueryLength(int maxQueryLength) {
        this.maxQueryLength = maxQueryLength;
    }
}
