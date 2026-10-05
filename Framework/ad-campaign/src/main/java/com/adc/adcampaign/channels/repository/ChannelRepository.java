package com.adc.adcampaign.channels.repository;

import com.adc.adcampaign.channels.entity.Channel;
import com.adc.adcampaign.channels.entity.ChannelType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChannelRepository extends JpaRepository<Channel, Long> {
    List<Channel> findByTenantId(Long tenantId);
    Optional<Channel> findByTenantIdAndType(Long tenantId, ChannelType type);
}
