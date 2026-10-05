package com.adc.adcampaign.finance.payments;

import com.adc.adcampaign.campaign.repository.AdvertiserRepository;
import com.adc.adcampaign.finance.payments.dto.PaymentMethodRequest;
import com.adc.adcampaign.finance.payments.entity.PaymentMethod;
import com.adc.adcampaign.finance.payments.repository.PaymentMethodRepository;
import com.adc.adcampaign.foundation.error.NotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PaymentMethodService {

    private static final String DEFAULT_PROVIDER = "MOCK";

    private final PaymentMethodRepository paymentMethodRepository;
    private final AdvertiserRepository advertiserRepository;

    public PaymentMethodService(PaymentMethodRepository paymentMethodRepository,
                                 AdvertiserRepository advertiserRepository) {
        this.paymentMethodRepository = paymentMethodRepository;
        this.advertiserRepository = advertiserRepository;
    }

    public PaymentMethod register(Long tenantId, PaymentMethodRequest request) {
        if (advertiserRepository.findByIdAndTenantId(request.getAdvertiserId(), tenantId).isEmpty()) {
            throw new NotFoundException("Advertiser not found: " + request.getAdvertiserId());
        }
        PaymentMethod method = PaymentMethod.builder()
                .tenantId(tenantId)
                .advertiserId(request.getAdvertiserId())
                .provider(request.getProvider() == null || request.getProvider().isBlank()
                        ? DEFAULT_PROVIDER : request.getProvider())
                .token(request.getToken())
                .brand(request.getBrand())
                .last4(request.getLast4())
                .expiryMonth(request.getExpiryMonth())
                .expiryYear(request.getExpiryYear())
                .build();
        return paymentMethodRepository.save(method);
    }

    public List<PaymentMethod> listForAdvertiser(Long tenantId, Long advertiserId) {
        return paymentMethodRepository.findByTenantIdAndAdvertiserId(tenantId, advertiserId);
    }

    public void delete(Long tenantId, Long id) {
        PaymentMethod method = paymentMethodRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Payment method not found: " + id));
        paymentMethodRepository.delete(method);
    }
}
