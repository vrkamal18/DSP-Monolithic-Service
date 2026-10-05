package com.adc.adcampaign.openrtb;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * A bounded, isolated pool for bid decisioning
 * (service/documents/01-service-architecture.md §9), separate from the
 * Tomcat threads serving management APIs so a slow report or connector sync
 * can't starve OpenRTB of a thread. A full queue rejects immediately
 * (AbortPolicy) instead of queuing indefinitely — "reject early when
 * overloaded" (service/documents/05-openrtb-and-bidding-specification.md §8).
 */
@Configuration
public class OpenRtbExecutorConfig {

    @Bean
    public ExecutorService openRtbExecutor(OpenRtbExecutorProperties properties) {
        return new ThreadPoolExecutor(
                properties.getCorePoolSize(),
                properties.getMaxPoolSize(),
                60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(properties.getQueueCapacity()),
                new ThreadPoolExecutor.AbortPolicy());
    }
}
