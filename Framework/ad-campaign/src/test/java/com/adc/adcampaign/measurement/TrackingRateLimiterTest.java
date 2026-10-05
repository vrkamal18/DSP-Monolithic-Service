package com.adc.adcampaign.measurement;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class TrackingRateLimiterTest {

    @Test
    void allowsUpToTheConfiguredMaxThenBlocks() {
        AtomicLong clock = new AtomicLong(0);
        TrackingRateLimiter limiter = new TrackingRateLimiter(3, 60_000, clock::get);

        assertThat(limiter.allow("1.2.3.4")).isTrue();
        assertThat(limiter.allow("1.2.3.4")).isTrue();
        assertThat(limiter.allow("1.2.3.4")).isTrue();
        assertThat(limiter.allow("1.2.3.4")).isFalse();
    }

    @Test
    void tracksEachKeyIndependently() {
        AtomicLong clock = new AtomicLong(0);
        TrackingRateLimiter limiter = new TrackingRateLimiter(1, 60_000, clock::get);

        assertThat(limiter.allow("1.2.3.4")).isTrue();
        assertThat(limiter.allow("5.6.7.8")).isTrue();
        assertThat(limiter.allow("1.2.3.4")).isFalse();
    }

    @Test
    void allowsAgainOnceTheWindowElapses() {
        AtomicLong clock = new AtomicLong(0);
        TrackingRateLimiter limiter = new TrackingRateLimiter(1, 60_000, clock::get);

        assertThat(limiter.allow("1.2.3.4")).isTrue();
        assertThat(limiter.allow("1.2.3.4")).isFalse();

        clock.set(60_001);

        assertThat(limiter.allow("1.2.3.4")).isTrue();
    }
}
