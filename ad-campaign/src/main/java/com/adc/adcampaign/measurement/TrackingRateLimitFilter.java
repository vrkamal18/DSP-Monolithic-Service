package com.adc.adcampaign.measurement;

import com.adc.adcampaign.foundation.web.FilterResponses;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Abuse control for the public tracking endpoints: caps query-string length
 * and per-client request rate. Deliberately not Spring Security — a plain
 * servlet filter scoped to one path, mirroring
 * {@code exchange.bidscube.BidsCubeAuthFilter}. Rejections go through
 * {@link FilterResponses#reject} — see its javadoc for why not
 * {@code sendError}.
 */
public class TrackingRateLimitFilter extends OncePerRequestFilter {

    private final TrackingProperties properties;
    private final TrackingRateLimiter rateLimiter;

    public TrackingRateLimitFilter(TrackingProperties properties) {
        this.properties = properties;
        this.rateLimiter = new TrackingRateLimiter(properties.getMaxRequestsPerWindow(),
                properties.getWindowSeconds() * 1000L);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String query = request.getQueryString();
        if (query != null && query.length() > properties.getMaxQueryLength()) {
            FilterResponses.reject(response, HttpServletResponse.SC_BAD_REQUEST, "Query string too long");
            return;
        }

        if (!rateLimiter.allow(request.getRemoteAddr())) {
            FilterResponses.reject(response, 429, "Rate limit exceeded");
            return;
        }

        chain.doFilter(request, response);
    }
}
