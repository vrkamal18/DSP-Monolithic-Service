package com.adc.adcampaign.openrtb;

import com.adc.adcampaign.bidding.model.NoBidReason;
import com.adc.adcampaign.openrtb.dto.BidRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Business-level validation beyond bean-validation annotations: supported
 * media type and currency. Structural required-field checks (id, imp[])
 * are enforced by {@code jakarta.validation} on {@link BidRequest} itself.
 */
@Component
public class OpenRtbRequestValidator {

    private static final List<String> SUPPORTED_CURRENCIES = List.of("USD");

    public Optional<NoBidReason> validate(BidRequest request) {
        boolean hasBanner = request.getImp().stream().anyMatch(imp -> imp.getBanner() != null);
        if (!hasBanner) {
            return Optional.of(NoBidReason.UNSUPPORTED_MEDIA);
        }

        if (request.getCur() != null && !request.getCur().isEmpty()
                && request.getCur().stream().noneMatch(SUPPORTED_CURRENCIES::contains)) {
            return Optional.of(NoBidReason.UNSUPPORTED_CURRENCY);
        }

        if (request.getSite() == null && request.getApp() == null) {
            return Optional.of(NoBidReason.INVALID_REQUEST);
        }

        return Optional.empty();
    }
}
