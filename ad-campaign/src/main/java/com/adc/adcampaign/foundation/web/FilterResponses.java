package com.adc.adcampaign.foundation.web;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Writes a rejection response directly from a plain servlet {@code Filter}
 * (not Spring Security) — never via {@link HttpServletResponse#sendError},
 * which triggers the container's internal error-dispatch to {@code /error}.
 * That dispatch re-enters Spring Security's filter chain a second time, and
 * since {@code /error} isn't on any permit-all list, it gets rejected with
 * its own 401, silently overwriting the status the filter actually intended
 * (first found in {@code measurement.TrackingRateLimitFilter}, then again in
 * {@code exchange.bidscube.BidsCubeAuthFilter} — centralized here so a third
 * filter doesn't reintroduce the same bug).
 */
public final class FilterResponses {

    private FilterResponses() {
    }

    public static void reject(HttpServletResponse response, int status, String detail) throws IOException {
        response.setStatus(status);
        response.setContentType("application/problem+json");
        response.getWriter().write("{\"status\":" + status + ",\"detail\":\"" + detail + "\"}");
    }
}
