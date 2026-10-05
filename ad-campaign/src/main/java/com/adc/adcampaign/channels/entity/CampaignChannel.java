package com.adc.adcampaign.channels.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * A campaign's association with a channel type. Owned by Channels, not
 * Campaign — Campaign is only referenced by id, per the
 * "channels -> campaign contracts" dependency direction in
 * service/documents/01-service-architecture.md §6.
 */
@Entity
@Table(name = "campaign_channels", uniqueConstraints = @UniqueConstraint(
        name = "uk_campaign_channel", columnNames = {"tenant_id", "campaign_id", "channel_type"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampaignChannel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", nullable = false)
    private ChannelType channelType;
}
