package com.adc.adcampaign.channels;

import com.adc.adcampaign.channels.entity.Channel;
import com.adc.adcampaign.channels.entity.ChannelType;
import com.adc.adcampaign.channels.repository.ChannelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class ChannelServiceTest {

    private static final Long TENANT_ID = 1L;

    @Mock private ChannelRepository channelRepository;

    private ChannelService channelService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        channelService = new ChannelService(channelRepository);
    }

    @Test
    void unconfiguredChannelTypeDefaultsToDisabled() {
        when(channelRepository.findByTenantId(TENANT_ID)).thenReturn(List.of());

        assertThat(channelService.isEnabled(TENANT_ID, ChannelType.VIDEO)).isFalse();
        List<Channel> all = channelService.listForTenant(TENANT_ID);
        assertThat(all).hasSize(ChannelType.values().length);
        assertThat(all).allMatch(c -> !c.isEnabled());
    }

    @Test
    void setEnabledUpsertsAndIsReflectedByIsEnabled() {
        when(channelRepository.findByTenantIdAndType(TENANT_ID, ChannelType.VIDEO)).thenReturn(Optional.empty());
        when(channelRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Channel result = channelService.setEnabled(TENANT_ID, ChannelType.VIDEO, true);

        assertThat(result.isEnabled()).isTrue();
        assertThat(result.getType()).isEqualTo(ChannelType.VIDEO);
    }
}
