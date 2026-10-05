package com.adc.adcampaign.measurement;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * A fixed-window per-key request counter. In-memory, single-instance, no
 * eviction of stale keys — good enough for a monolith today, same
 * simplicity/documented-limitation spirit as
 * {@code exchange.bidscube.BidsCubeAuthFilter}.
 */
public class TrackingRateLimiter {

    private final int maxRequestsPerWindow;
    private final long windowMillis;
    private final LongSupplier clock;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public TrackingRateLimiter(int maxRequestsPerWindow, long windowMillis) {
        this(maxRequestsPerWindow, windowMillis, System::currentTimeMillis);
    }

    TrackingRateLimiter(int maxRequestsPerWindow, long windowMillis, LongSupplier clock) {
        this.maxRequestsPerWindow = maxRequestsPerWindow;
        this.windowMillis = windowMillis;
        this.clock = clock;
    }

    public boolean allow(String key) {
        long now = clock.getAsLong();
        Window window = windows.computeIfAbsent(key, k -> new Window(now));
        synchronized (window) {
            if (now - window.start >= windowMillis) {
                window.start = now;
                window.count = 0;
            }
            window.count++;
            return window.count <= maxRequestsPerWindow;
        }
    }

    private static final class Window {
        private long start;
        private int count;

        private Window(long start) {
            this.start = start;
        }
    }
}
