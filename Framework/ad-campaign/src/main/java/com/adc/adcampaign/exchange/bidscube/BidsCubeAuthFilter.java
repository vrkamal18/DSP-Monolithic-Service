package com.adc.adcampaign.exchange.bidscube;

import com.adc.adcampaign.foundation.web.FilterResponses;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Authenticates and bounds every request on the BidsCube exchange path.
 * Deliberately not Spring Security: this is a single shared-secret header
 * check scoped to one path, not a general authentication/authorization
 * system, so a full security framework isn't warranted here. Rejections go
 * through {@link FilterResponses#reject} — see its javadoc for why not
 * {@code sendError} (this filter's own unit test, which exercises it with a
 * mocked FilterChain rather than a real container, never caught the bug
 * sendError caused here in practice).
 */
public class BidsCubeAuthFilter extends OncePerRequestFilter {

    private final BidsCubeProperties properties;

    public BidsCubeAuthFilter(BidsCubeProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long contentLength = request.getContentLengthLong();
        if (contentLength > properties.getMaxRequestBodyBytes()) {
            FilterResponses.reject(response, HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "Request body too large");
            return;
        }

        String provided = request.getHeader(properties.getAuthHeaderName());
        String expected = properties.getAuthSharedSecret();
        if (expected == null || expected.isBlank() || !expected.equals(provided)) {
            FilterResponses.reject(response, HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid exchange credentials");
            return;
        }

        chain.doFilter(request, response);
    }
}
