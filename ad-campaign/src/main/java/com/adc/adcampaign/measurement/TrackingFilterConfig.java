package com.adc.adcampaign.measurement;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TrackingFilterConfig {

    @Bean
    public FilterRegistrationBean<TrackingRateLimitFilter> trackingRateLimitFilter(TrackingProperties properties) {
        FilterRegistrationBean<TrackingRateLimitFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new TrackingRateLimitFilter(properties));
        registration.addUrlPatterns("/t/*");
        registration.setName("trackingRateLimitFilter");
        registration.setOrder(1);
        return registration;
    }
}
