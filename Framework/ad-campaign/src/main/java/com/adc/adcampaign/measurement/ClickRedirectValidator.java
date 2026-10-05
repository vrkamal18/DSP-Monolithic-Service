package com.adc.adcampaign.measurement;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Validates a click-redirect destination is a well-formed absolute http(s)
 * URL, per service/documents/06-security-and-multitenancy.md §10 ("Validate
 * click destinations against creative/campaign configuration to avoid an
 * open redirect"). Creative has no registered landing-page URL to allow-list
 * against yet, so this is scheme/format validation only — see the
 * measurement plan for why full allow-listing is deferred.
 */
@Component
public class ClickRedirectValidator {

    public boolean isSafe(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
                return false;
            }
            return uri.getHost() != null && !uri.getHost().isBlank();
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
