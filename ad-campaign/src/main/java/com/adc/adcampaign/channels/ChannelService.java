package com.adc.adcampaign.channels;

import com.adc.adcampaign.channels.entity.Channel;
import com.adc.adcampaign.channels.entity.ChannelType;
import com.adc.adcampaign.channels.repository.ChannelRepository;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChannelService {

    private final ChannelRepository channelRepository;

    public ChannelService(ChannelRepository channelRepository) {
        this.channelRepository = channelRepository;
    }

    /** All 7 channel types for the tenant, defaulting to disabled when unconfigured. */
    public List<Channel> listForTenant(Long tenantId) {
        Map<ChannelType, Channel> configured = channelRepository.findByTenantId(tenantId).stream()
                .collect(Collectors.toMap(Channel::getType, c -> c));

        return Arrays.stream(ChannelType.values())
                .map(type -> configured.getOrDefault(type,
                        Channel.builder().tenantId(tenantId).type(type).enabled(false).build()))
                .toList();
    }

    public boolean isEnabled(Long tenantId, ChannelType type) {
        return channelRepository.findByTenantIdAndType(tenantId, type)
                .map(Channel::isEnabled)
                .orElse(false);
    }

    public Channel setEnabled(Long tenantId, ChannelType type, boolean enabled) {
        Channel channel = channelRepository.findByTenantIdAndType(tenantId, type)
                .orElseGet(() -> Channel.builder().tenantId(tenantId).type(type).build());
        channel.setEnabled(enabled);
        return channelRepository.save(channel);
    }
}
