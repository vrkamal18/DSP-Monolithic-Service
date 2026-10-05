package com.adc.adcampaign.finance.payments;

import com.adc.adcampaign.campaign.entity.Advertiser;
import com.adc.adcampaign.campaign.repository.AdvertiserRepository;
import com.adc.adcampaign.finance.payments.dto.PaymentMethodRequest;
import com.adc.adcampaign.finance.payments.entity.PaymentMethod;
import com.adc.adcampaign.finance.payments.repository.PaymentMethodRepository;
import com.adc.adcampaign.foundation.error.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentMethodServiceTest {

    private static final Long TENANT_ID = 1L;
    private static final Long OTHER_TENANT_ID = 2L;
    private static final Long ADVERTISER_ID = 5L;

    @Mock private PaymentMethodRepository paymentMethodRepository;
    @Mock private AdvertiserRepository advertiserRepository;

    private PaymentMethodService paymentMethodService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        paymentMethodService = new PaymentMethodService(paymentMethodRepository, advertiserRepository);
    }

    @Test
    void registerDefaultsProviderToMockAndSaves() {
        when(advertiserRepository.findByIdAndTenantId(ADVERTISER_ID, TENANT_ID))
                .thenReturn(Optional.of(Advertiser.builder().id(ADVERTISER_ID).build()));
        when(paymentMethodRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentMethodRequest request = new PaymentMethodRequest();
        request.setAdvertiserId(ADVERTISER_ID);
        request.setToken("tok_visa");
        request.setBrand("Visa");
        request.setLast4("4242");

        PaymentMethod result = paymentMethodService.register(TENANT_ID, request);

        assertThat(result.getProvider()).isEqualTo("MOCK");
        assertThat(result.getTenantId()).isEqualTo(TENANT_ID);
        verify(paymentMethodRepository).save(any());
    }

    @Test
    void registerRejectsAnAdvertiserFromAnotherTenant() {
        when(advertiserRepository.findByIdAndTenantId(ADVERTISER_ID, OTHER_TENANT_ID))
                .thenReturn(Optional.empty());

        PaymentMethodRequest request = new PaymentMethodRequest();
        request.setAdvertiserId(ADVERTISER_ID);
        request.setToken("tok_visa");

        assertThatThrownBy(() -> paymentMethodService.register(OTHER_TENANT_ID, request))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void listIsScopedToTenantAndAdvertiser() {
        when(paymentMethodRepository.findByTenantIdAndAdvertiserId(TENANT_ID, ADVERTISER_ID))
                .thenReturn(List.of(PaymentMethod.builder().id(1L).build()));

        List<PaymentMethod> result = paymentMethodService.listForAdvertiser(TENANT_ID, ADVERTISER_ID);

        assertThat(result).hasSize(1);
    }

    @Test
    void deleteRejectsAPaymentMethodFromAnotherTenant() {
        when(paymentMethodRepository.findByIdAndTenantId(3L, OTHER_TENANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentMethodService.delete(OTHER_TENANT_ID, 3L))
                .isInstanceOf(NotFoundException.class);
    }
}
