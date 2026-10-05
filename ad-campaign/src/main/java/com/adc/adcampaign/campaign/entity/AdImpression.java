package com.adc.adcampaign.campaign.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ad_impressions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdImpression {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creative_id")
    private Creative creative;

    private String location;
    private String ageGroup;
    private String deviceType;
    private String userId;

    @Column(name = "served_at", updatable = false)
    private LocalDateTime servedAt;

    @PrePersist
    public void setServedAt() {
        this.servedAt = LocalDateTime.now();
    }
}
