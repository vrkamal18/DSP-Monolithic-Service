package com.adc.adcampaign.channels.repository;

import com.adc.adcampaign.channels.entity.CampaignChannel;
import com.adc.adcampaign.channels.entity.ChannelType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CampaignChannelRepository extends JpaRepository<CampaignChannel, Long> {
    List<CampaignChannel> findByTenantIdAndCampaignId(Long tenantId, Long campaignId);
    Optional<CampaignChannel> findByTenantIdAndCampaignIdAndChannelType(
            Long tenantId, Long campaignId, ChannelType channelType);
}
