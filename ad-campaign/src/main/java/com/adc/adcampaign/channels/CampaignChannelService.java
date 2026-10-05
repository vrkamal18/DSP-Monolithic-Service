package com.adc.adcampaign.channels;

import com.adc.adcampaign.campaign.repository.CampaignRepository;
import com.adc.adcampaign.channels.entity.CampaignChannel;
import com.adc.adcampaign.channels.entity.ChannelType;
import com.adc.adcampaign.channels.repository.CampaignChannelRepository;
import com.adc.adcampaign.foundation.error.NotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CampaignChannelService {

    private final CampaignChannelRepository campaignChannelRepository;
    private final CampaignRepository campaignRepository;
    private final ChannelService channelService;

    public CampaignChannelService(CampaignChannelRepository campaignChannelRepository,
                                   CampaignRepository campaignRepository,
                                   ChannelService channelService) {
        this.campaignChannelRepository = campaignChannelRepository;
        this.campaignRepository = campaignRepository;
        this.channelService = channelService;
    }

    public CampaignChannel assign(Long tenantId, Long campaignId, ChannelType channelType) {
        campaignRepository.findByIdAndTenantId(campaignId, tenantId)
                .orElseThrow(() -> new NotFoundException("Campaign not found: " + campaignId));

        if (!channelService.isEnabled(tenantId, channelType)) {
            throw new IllegalStateException("Channel " + channelType + " is not enabled for this tenant");
        }

        return campaignChannelRepository.findByTenantIdAndCampaignIdAndChannelType(tenantId, campaignId, channelType)
                .orElseGet(() -> campaignChannelRepository.save(CampaignChannel.builder()
                        .tenantId(tenantId)
                        .campaignId(campaignId)
                        .channelType(channelType)
                        .build()));
    }

    public List<CampaignChannel> listForCampaign(Long tenantId, Long campaignId) {
        campaignRepository.findByIdAndTenantId(campaignId, tenantId)
                .orElseThrow(() -> new NotFoundException("Campaign not found: " + campaignId));
        return campaignChannelRepository.findByTenantIdAndCampaignId(tenantId, campaignId);
    }

    public void remove(Long tenantId, Long campaignId, ChannelType channelType) {
        CampaignChannel association = campaignChannelRepository
                .findByTenantIdAndCampaignIdAndChannelType(tenantId, campaignId, channelType)
                .orElseThrow(() -> new NotFoundException(
                        "Campaign " + campaignId + " is not associated with channel " + channelType));
        campaignChannelRepository.delete(association);
    }
}
