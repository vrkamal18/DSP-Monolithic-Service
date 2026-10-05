package com.adc.adcampaign.exchange.bidscube;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for the BidsCube exchange adapter. Values come from
 * {@code application.properties} / environment, never hardcoded — the
 * shared secret in particular must be supplied via an env-backed property
 * in every real environment.
 */
@Component
@ConfigurationProperties(prefix = "bidscube")
public class BidsCubeProperties {

    /** Header BidsCube is expected to send on every bid request. */
    private String authHeaderName = "X-BidsCube-Auth";

    /** Shared secret value expected in {@link #authHeaderName}. */
    private String authSharedSecret = "";

    /** Used when the incoming request omits tmax. */
    private long defaultTmaxMillis = 150;

    /** Reserved off the request tmax for our own network/serialization overhead. */
    private long tmaxSafetyMarginMillis = 20;

    /** Requests with a Content-Length above this are rejected before parsing. */
    private long maxRequestBodyBytes = 65_536;

    /**
     * Base URL BidsCube can call back to report a win (the {@code burl}).
     * Left blank until BidsCube's notification behavior/macros are confirmed;
     * no burl is included in the response while blank.
     */
    private String winNoticeBaseUrl = "";

    /**
     * The tenant BidsCube's traffic is decisioned against. One exchange
     * connection maps to one operator tenant; a multi-tenant BidsCube seat
     * mapping is not needed yet.
     */
    private Long tenantId;

    public String getAuthHeaderName() {
        return authHeaderName;
    }

    public void setAuthHeaderName(String authHeaderName) {
        this.authHeaderName = authHeaderName;
    }

    public String getAuthSharedSecret() {
        return authSharedSecret;
    }

    public void setAuthSharedSecret(String authSharedSecret) {
        this.authSharedSecret = authSharedSecret;
    }

    public long getDefaultTmaxMillis() {
        return defaultTmaxMillis;
    }

    public void setDefaultTmaxMillis(long defaultTmaxMillis) {
        this.defaultTmaxMillis = defaultTmaxMillis;
    }

    public long getTmaxSafetyMarginMillis() {
        return tmaxSafetyMarginMillis;
    }

    public void setTmaxSafetyMarginMillis(long tmaxSafetyMarginMillis) {
        this.tmaxSafetyMarginMillis = tmaxSafetyMarginMillis;
    }

    public long getMaxRequestBodyBytes() {
        return maxRequestBodyBytes;
    }

    public void setMaxRequestBodyBytes(long maxRequestBodyBytes) {
        this.maxRequestBodyBytes = maxRequestBodyBytes;
    }

    public String getWinNoticeBaseUrl() {
        return winNoticeBaseUrl;
    }

    public void setWinNoticeBaseUrl(String winNoticeBaseUrl) {
        this.winNoticeBaseUrl = winNoticeBaseUrl;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }
}
