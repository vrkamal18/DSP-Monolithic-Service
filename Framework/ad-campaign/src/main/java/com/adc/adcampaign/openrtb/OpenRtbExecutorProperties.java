package com.adc.adcampaign.openrtb;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Sizing for the dedicated bid-decisioning executor, shared by every OpenRTB
 * exchange adapter. Dev-scale defaults — tune once real exchange QPS is
 * known, same caveat the tmax defaults already carry.
 */
@Component
@ConfigurationProperties(prefix = "openrtb.executor")
public class OpenRtbExecutorProperties {

    private int corePoolSize = 8;
    private int maxPoolSize = 16;
    private int queueCapacity = 50;

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public void setCorePoolSize(int corePoolSize) {
        this.corePoolSize = corePoolSize;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }
}
