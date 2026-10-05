package com.adc.adcampaign.channels;

import com.adc.adcampaign.campaign.entity.Campaign;
import com.adc.adcampaign.campaign.repository.CampaignRepository;
import com.adc.adcampaign.channels.entity.CampaignChannel;
import com.adc.adcampaign.channels.entity.ChannelType;
import com.adc.adcampaign.channels.repository.CampaignChannelRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CampaignChannelServiceTest {

    private static final Long TENANT_ID = 1L;
    private static final Long CAMPAIGN_ID = 10L;

    @Mock private CampaignChannelRepository campaignChannelRepository;
    @Mock private CampaignRepository campaignRepository;
    @Mock private ChannelService channelService;

    private CampaignChannelService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new CampaignChannelService(campaignChannelRepository, campaignRepository, channelService);
    }

    @Test
    void assignSucceedsWhenChannelEnabledAndCampaignOwnedByTenant() {
        when(campaignRepository.findByIdAndTenantId(CAMPAIGN_ID, TENANT_ID))
                .thenReturn(Optional.of(Campaign.builder().id(CAMPAIGN_ID).build()));
        when(channelService.isEnabled(TENANT_ID, ChannelType.VIDEO)).thenReturn(true);
        when(campaignChannelRepository.findByTenantIdAndCampaignIdAndChannelType(TENANT_ID, CAMPAIGN_ID, ChannelType.VIDEO))
                .thenReturn(Optional.empty());
        when(campaignChannelRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CampaignChannel result = service.assign(TENANT_ID, CAMPAIGN_ID, ChannelType.VIDEO);

        assertThat(result.getChannelType()).isEqualTo(ChannelType.VIDEO);
    }

    @Test
    void assignRejectsADisabledChannel() {
        when(campaignRepository.findByIdAndTenantId(CAMPAIGN_ID, TENANT_ID))
                .thenReturn(Optional.of(Campaign.builder().id(CAMPAIGN_ID).build()));
        when(channelService.isEnabled(TENANT_ID, ChannelType.VIDEO)).thenReturn(false);

        assertThatThrownBy(() -> service.assign(TENANT_ID, CAMPAIGN_ID, ChannelType.VIDEO))
                .isInstanceOf(IllegalStateException.class);
        verify(campaignChannelRepository, never()).save(any());
    }

    @Test
    void assignRejectsACampaignFromAnotherTenant() {
        when(campaignRepository.findByIdAndTenantId(CAMPAIGN_ID, TENANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assign(TENANT_ID, CAMPAIGN_ID, ChannelType.VIDEO))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void assignIsIdempotentWhenAlreadyAssigned() {
        CampaignChannel existing = CampaignChannel.builder().id(1L).tenantId(TENANT_ID)
                .campaignId(CAMPAIGN_ID).channelType(ChannelType.VIDEO).build();
        when(campaignRepository.findByIdAndTenantId(CAMPAIGN_ID, TENANT_ID))
                .thenReturn(Optional.of(Campaign.builder().id(CAMPAIGN_ID).build()));
        when(channelService.isEnabled(TENANT_ID, ChannelType.VIDEO)).thenReturn(true);
        when(campaignChannelRepository.findByTenantIdAndCampaignIdAndChannelType(TENANT_ID, CAMPAIGN_ID, ChannelType.VIDEO))
                .thenReturn(Optional.of(existing));

        CampaignChannel result = service.assign(TENANT_ID, CAMPAIGN_ID, ChannelType.VIDEO);

        assertThat(result.getId()).isEqualTo(1L);
        verify(campaignChannelRepository, never()).save(any());
    }

    @Test
    void listAndRemoveWork() {
        when(campaignRepository.findByIdAndTenantId(CAMPAIGN_ID, TENANT_ID))
                .thenReturn(Optional.of(Campaign.builder().id(CAMPAIGN_ID).build()));
        CampaignChannel existing = CampaignChannel.builder().id(1L).tenantId(TENANT_ID)
                .campaignId(CAMPAIGN_ID).channelType(ChannelType.VIDEO).build();
        when(campaignChannelRepository.findByTenantIdAndCampaignId(TENANT_ID, CAMPAIGN_ID))
                .thenReturn(List.of(existing));
        when(campaignChannelRepository.findByTenantIdAndCampaignIdAndChannelType(TENANT_ID, CAMPAIGN_ID, ChannelType.VIDEO))
                .thenReturn(Optional.of(existing));

        assertThat(service.listForCampaign(TENANT_ID, CAMPAIGN_ID)).hasSize(1);

        service.remove(TENANT_ID, CAMPAIGN_ID, ChannelType.VIDEO);
        verify(campaignChannelRepository).delete(existing);
    }
}
