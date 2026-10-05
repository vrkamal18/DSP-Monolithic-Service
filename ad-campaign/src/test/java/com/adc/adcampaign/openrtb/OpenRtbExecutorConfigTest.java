package com.adc.adcampaign.openrtb;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import static org.assertj.core.api.Assertions.assertThat;

class OpenRtbExecutorConfigTest {

    @Test
    void buildsABoundedExecutorWithTheConfiguredSizesAndAbortPolicy() {
        OpenRtbExecutorProperties properties = new OpenRtbExecutorProperties();
        properties.setCorePoolSize(2);
        properties.setMaxPoolSize(4);
        properties.setQueueCapacity(10);

        ExecutorService executor = new OpenRtbExecutorConfig().openRtbExecutor(properties);

        assertThat(executor).isInstanceOf(ThreadPoolExecutor.class);
        ThreadPoolExecutor pool = (ThreadPoolExecutor) executor;
        assertThat(pool.getCorePoolSize()).isEqualTo(2);
        assertThat(pool.getMaximumPoolSize()).isEqualTo(4);
        assertThat(pool.getQueue()).isInstanceOf(ArrayBlockingQueue.class);
        assertThat(pool.getQueue().remainingCapacity()).isEqualTo(10);
        assertThat(pool.getRejectedExecutionHandler()).isInstanceOf(ThreadPoolExecutor.AbortPolicy.class);

        executor.shutdownNow();
    }
}
