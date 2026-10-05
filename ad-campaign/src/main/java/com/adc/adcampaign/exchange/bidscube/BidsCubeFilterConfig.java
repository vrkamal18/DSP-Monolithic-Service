package com.adc.adcampaign.exchange.bidscube;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BidsCubeFilterConfig {

    /**
     * Scoped to the bid-request path only. The win-notice path
     * ({@code /exchange/bidscube/win}) is deliberately NOT covered — a real
     * exchange fires burl as a plain GET pixel with no custom headers, so
     * this header-based check can't protect it (see
     * BidsCubeNotificationController's Javadoc for its own security model).
     */
    @Bean
    public FilterRegistrationBean<BidsCubeAuthFilter> bidsCubeAuthFilter(BidsCubeProperties properties) {
        FilterRegistrationBean<BidsCubeAuthFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new BidsCubeAuthFilter(properties));
        registration.addUrlPatterns("/exchange/bidscube/openrtb2/*");
        registration.setName("bidsCubeAuthFilter");
        registration.setOrder(1);
        return registration;
    }
}
